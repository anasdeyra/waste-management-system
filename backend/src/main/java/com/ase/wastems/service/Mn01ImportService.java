package com.ase.wastems.service;

import com.ase.wastems.config.ServiceAreaProperties;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.SourceSnapshot;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent import of the included MN01 basket and depot snapshots (US-33 groundwork, D-019,
 * D-043). Rows are upserted on source dataset + identifier. Only the source ownership label of
 * an existing basket is refreshed; planner-editable fields (name, coordinates, status, demand)
 * are never overwritten. New baskets receive one synthetic demand entry.
 */
@Service
public class Mn01ImportService {

    public static final String BASKET_DATASET = "8znf-7b2c";
    public static final String DEPOT_DATASET = "xw3j-2yxf";
    public static final String BASKET_FILE = "data/nyc-mn01-baskets.csv";
    public static final String DEPOT_FILE = "data/nyc-mn01-depot.csv";
    public static final String SYNTHETIC_CATEGORY = "Mixed litter";
    static final String SYNTHETIC_CATEGORY_DESCRIPTION =
            "Street litter-basket refuse (synthetic demo category)";

    private static final int MAX_WARNINGS = 20;
    private static final int MAX_NAME_LENGTH = 120;

    private final CollectionPointRepository points;
    private final DepotRepository depots;
    private final WasteCategoryRepository categories;
    private final SourceSnapshotRepository snapshots;
    private final ServiceAreaProperties area;
    private final AuditService audit;
    private final AtomicReference<Mn01ImportReport> lastReport = new AtomicReference<>();

    public Mn01ImportService(
            CollectionPointRepository points,
            DepotRepository depots,
            WasteCategoryRepository categories,
            SourceSnapshotRepository snapshots,
            ServiceAreaProperties area,
            AuditService audit) {
        this.audit = audit;
        this.points = points;
        this.depots = depots;
        this.categories = categories;
        this.snapshots = snapshots;
        this.area = area;
    }

    /** Collects at most {@value #MAX_WARNINGS} warnings, then one "omitted" marker. */
    private static final class Warnings {
        private final List<String> list = new ArrayList<>();
        private boolean overflow;

        void add(String message) {
            if (list.size() < MAX_WARNINGS) {
                list.add(message);
            } else {
                overflow = true;
            }
        }

        List<String> toList() {
            List<String> out = new ArrayList<>(list);
            if (overflow) {
                out.add("... more warnings omitted");
            }
            return out;
        }
    }

    /** A source file read from the classpath: bytes hashed, text decoded as UTF-8. */
    private record SourceFile(String path, String sha256, List<Map<String, String>> rows) {
        String snapshotId(String dataset) {
            return dataset + ":" + rows.size() + ":" + sha256.substring(0, 12);
        }
    }

    @Transactional
    public Mn01ImportReport importAll(ImportTrigger trigger) {
        Instant started = Instant.now();
        Warnings warnings = new Warnings();
        SourceFile basketFile = readSource(BASKET_FILE);
        SourceFile depotFile = readSource(DEPOT_FILE);

        WasteCategory category = syntheticCategory(warnings);
        DatasetImportResult baskets = importBaskets(basketFile, category, warnings);
        DatasetImportResult depotResult = importDepots(depotFile, warnings);

        recordSnapshot(BASKET_DATASET, basketFile);
        recordSnapshot(DEPOT_DATASET, depotFile);

        Mn01ImportReport report =
                new Mn01ImportReport(
                        trigger,
                        started,
                        Instant.now(),
                        baskets,
                        depotResult,
                        baskets.created(),
                        SYNTHETIC_CATEGORY,
                        warnings.toList());
        lastReport.set(report);
        audit.record(
                AuditAction.DATA_IMPORTED,
                AuditEntityType.DATA_IMPORT,
                baskets.snapshotId(),
                trigger
                        + ": baskets created "
                        + baskets.created()
                        + ", updated "
                        + baskets.updated()
                        + ", existing "
                        + baskets.existing()
                        + ", skipped "
                        + baskets.skipped()
                        + "; depots created "
                        + depotResult.created()
                        + ", existing "
                        + depotResult.existing()
                        + "; warnings "
                        + report.warnings().size());
        return report;
    }

    public Optional<Mn01ImportReport> lastReport() {
        return Optional.ofNullable(lastReport.get());
    }

    @Transactional(readOnly = true)
    public List<SourceSnapshot> snapshots() {
        return snapshots.findAllByOrderBySourceDatasetAsc();
    }

    private WasteCategory syntheticCategory(Warnings warnings) {
        Optional<WasteCategory> existing = categories.findByNameIgnoreCase(SYNTHETIC_CATEGORY);
        if (existing.isPresent()) {
            if (!existing.get().isActive()) {
                warnings.add(
                        "category '"
                                + SYNTHETIC_CATEGORY
                                + "' is inactive; synthetic demand is still attached to it");
            }
            return existing.get();
        }
        return categories.save(new WasteCategory(SYNTHETIC_CATEGORY, SYNTHETIC_CATEGORY_DESCRIPTION));
    }

    private DatasetImportResult importBaskets(
            SourceFile file, WasteCategory category, Warnings warnings) {
        Map<String, CollectionPoint> known = new HashMap<>();
        for (CollectionPoint p : points.findBySourceDataset(BASKET_DATASET)) {
            known.put(p.getSourceIdentifier(), p);
        }
        Set<String> seen = new HashSet<>();
        List<CollectionPoint> created = new ArrayList<>();
        int existing = 0;
        int updated = 0;
        int skipped = 0;
        int dsny = 0;
        int rowNumber = 0;
        for (Map<String, String> row : file.rows()) {
            rowNumber++;
            String id = value(row, "basketid");
            String type = value(row, "baskettype");
            if (type.startsWith(CollectionPoint.DSNY_TYPE_PREFIX)) {
                dsny++;
            }
            String label = id.isEmpty() ? "row " + rowNumber : id;
            String problem = null;
            double lon = 0;
            double lat = 0;
            if (id.isEmpty()) {
                problem = "blank basketid";
            } else if (type.isEmpty()) {
                problem = "blank baskettype";
            } else if (!seen.add(id)) {
                problem = "duplicate basketid in file";
            } else {
                try {
                    lon = Double.parseDouble(value(row, "longitude"));
                    lat = Double.parseDouble(value(row, "latitude"));
                    if (!area.contains(lon, lat)) {
                        problem = "coordinates outside the service area";
                    }
                } catch (NumberFormatException e) {
                    problem = "unparsable coordinates";
                }
            }
            if (problem != null) {
                skipped++;
                warnings.add("basket " + label + ": " + problem);
                continue;
            }
            CollectionPoint p = known.get(id);
            if (p != null) {
                existing++;
                if (!type.equals(p.getSourceOwnershipType())) {
                    p.setSourceOwnershipType(type);
                    updated++;
                }
                continue;
            }
            CollectionPoint fresh =
                    new CollectionPoint(
                            basketName(value(row, "streetname1"), value(row, "streetname2"), id),
                            lon,
                            lat,
                            DataOrigin.IMPORTED_REAL);
            fresh.setSource(BASKET_DATASET, id, type);
            fresh.addDemand(new DemandEntry(category, syntheticDemandKg(id), DataOrigin.SYNTHETIC));
            created.add(fresh);
        }
        points.saveAll(created);
        return new DatasetImportResult(
                BASKET_DATASET,
                file.path(),
                file.rows().size(),
                dsny,
                created.size(),
                updated,
                existing,
                skipped,
                file.sha256(),
                file.snapshotId(BASKET_DATASET));
    }

    private DatasetImportResult importDepots(SourceFile file, Warnings warnings) {
        int created = 0;
        int existing = 0;
        int skipped = 0;
        int rowNumber = 0;
        for (Map<String, String> row : file.rows()) {
            rowNumber++;
            String id = value(row, "name");
            try {
                if (id.isEmpty()) {
                    throw new IllegalArgumentException("blank name");
                }
                double[] lonLat = SimpleCsv.parseWktPoint(value(row, "point"));
                if (!area.contains(lonLat[0], lonLat[1])) {
                    throw new IllegalArgumentException("coordinates outside the service area");
                }
                if (depots.findBySourceDatasetAndSourceIdentifier(DEPOT_DATASET, id).isPresent()) {
                    existing++;
                } else {
                    depots.save(
                            new Depot(
                                    id + " Garage, " + value(row, "address"),
                                    lonLat[0],
                                    lonLat[1],
                                    DataOrigin.IMPORTED_REAL,
                                    DEPOT_DATASET,
                                    id));
                    created++;
                }
            } catch (IllegalArgumentException e) {
                skipped++;
                warnings.add("depot " + (id.isEmpty() ? "row " + rowNumber : id) + ": " + e.getMessage());
            }
        }
        return new DatasetImportResult(
                DEPOT_DATASET,
                file.path(),
                file.rows().size(),
                null,
                created,
                0,
                existing,
                skipped,
                file.sha256(),
                file.snapshotId(DEPOT_DATASET));
    }

    /** New snapshot, replaced when the checksum changed, untouched when identical. */
    private void recordSnapshot(String dataset, SourceFile file) {
        String id = file.snapshotId(dataset);
        Optional<SourceSnapshot> current = snapshots.findBySourceDataset(dataset);
        if (current.isEmpty()) {
            snapshots.save(
                    new SourceSnapshot(
                            dataset, file.path(), file.rows().size(), file.sha256(), id, Instant.now()));
        } else if (!current.get().getSha256().equals(file.sha256())) {
            current.get().update(file.path(), file.rows().size(), file.sha256(), id, Instant.now());
        }
    }

    private static SourceFile readSource(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return new SourceFile(path, sha, SimpleCsv.parse(new String(bytes, StandardCharsets.UTF_8)));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot read source snapshot " + path, e);
        }
    }

    private static String value(Map<String, String> row, String column) {
        String v = row.get(column);
        if (v == null && !row.containsKey(column)) {
            throw new IllegalStateException("Missing column '" + column + "' in source snapshot");
        }
        return v == null ? "" : v.trim();
    }

    /** D-043: {@code STREET1 & STREET2}, else the non-blank street, else {@code Basket <id>}. */
    public static String basketName(String street1, String street2, String basketId) {
        String a = street1 == null ? "" : street1.trim();
        String b = street2 == null ? "" : street2.trim();
        String name;
        if (!a.isEmpty() && !b.isEmpty()) {
            name = a + " & " + b;
        } else if (!a.isEmpty()) {
            name = a;
        } else if (!b.isEmpty()) {
            name = b;
        } else {
            name = "Basket " + basketId;
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    /** Deterministic synthetic demand: an integer from 10 to 30 kg derived from the basket id. */
    static BigDecimal syntheticDemandKg(String basketId) {
        return BigDecimal.valueOf(10 + Math.floorMod(basketId.hashCode(), 21));
    }
}
