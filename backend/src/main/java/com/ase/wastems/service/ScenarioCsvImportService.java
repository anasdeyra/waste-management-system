package com.ase.wastems.service;

import com.ase.wastems.config.ServiceAreaProperties;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditOutcome;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.ScenarioKind;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationBatchRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import com.ase.wastems.routing.DirectedCostMatrixProvider;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Imports evaluation scenarios from a CSV (US-37, docs/evaluation-csv.md section 3). The whole
 * file is validated first; any error imports nothing. Valid scenarios are evaluated in memory and
 * the batch, its runs and one audit event are saved in one transaction. Imported points must
 * already exist. Operational data is never created or changed.
 */
@Service
public class ScenarioCsvImportService {

    public static final int MAX_BYTES = 1_048_576;
    public static final int MAX_ROWS = 5000;
    public static final int MAX_SCENARIOS = 10;
    public static final int MAX_POINTS = 100;

    private static final List<String> REQUIRED = List.of("scenario_key", "input_order", "longitude", "latitude",
            "category", "demand_kg", "vehicle_capacity_kg");
    private static final Set<String> KNOWN = new HashSet<>(List.of("scenario_key", "input_order", "point_id",
            "source_dataset", "source_identifier", "longitude", "latitude", "category", "demand_kg",
            "vehicle_capacity_kg", "demand_profile", "seed"));
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9._-]{1,80}");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?\\d+(\\.\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("\\d{1,19}");
    private static final BigDecimal MAX_KG = new BigDecimal("100000");

    private final CollectionPointRepository points;
    private final WasteCategoryRepository categories;
    private final DepotRepository depots;
    private final SourceSnapshotRepository snapshots;
    private final EvaluationService evaluations;
    private final EvaluationRunRepository runs;
    private final EvaluationBatchRepository batches;
    private final DirectedCostMatrixProvider matrices;
    private final ServiceAreaProperties area;
    private final AuditService audit;
    private final TransactionTemplate tx;

    public ScenarioCsvImportService(CollectionPointRepository points, WasteCategoryRepository categories,
            DepotRepository depots, SourceSnapshotRepository snapshots, EvaluationService evaluations,
            EvaluationRunRepository runs, EvaluationBatchRepository batches, DirectedCostMatrixProvider matrices,
            ServiceAreaProperties area, AuditService audit, PlatformTransactionManager txManager) {
        this.points = points;
        this.categories = categories;
        this.depots = depots;
        this.snapshots = snapshots;
        this.evaluations = evaluations;
        this.runs = runs;
        this.batches = batches;
        this.matrices = matrices;
        this.area = area;
        this.audit = audit;
        this.tx = new TransactionTemplate(txManager);
    }

    /** One data row that passed every field check. */
    private record Row(int row, String key, int inputOrder, CollectionPoint point, String ref, double lon,
            double lat, WasteCategory category, BigDecimal demand, BigDecimal capacity, String profile, Long seed) {}

    private static final class Errors {
        final List<CsvRowError> list = new ArrayList<>();

        void add(int row, String column, String message) {
            list.add(new CsvRowError(row, column, message));
        }
    }

    public EvaluationBatch importCsv(byte[] body, String fileName, String label, String username) {
        if (fileName != null && fileName.length() > 200) {
            throw new InvalidRequestException("fileName", "must be at most 200 characters");
        }
        if (label != null && label.length() > 120) {
            throw new InvalidRequestException("label", "must be at most 120 characters");
        }
        if (body == null || body.length == 0) {
            throw new InvalidRequestException("file", "The CSV file is empty.");
        }
        if (body.length > MAX_BYTES) {
            throw new InvalidRequestException("file", "The CSV file exceeds " + MAX_BYTES + " bytes.");
        }
        String text = new String(body, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        List<List<String>> records = new ArrayList<>();
        for (List<String> r : SimpleCsv.records(text)) {
            if (!(r.size() == 1 && r.get(0).isEmpty())) {
                records.add(r);
            }
        }
        Errors errors = new Errors();
        Map<String, Integer> columns = header(records, errors);
        List<Scenario> scenarios = List.of();
        int dataRows = Math.max(0, records.size() - 1);
        if (errors.list.isEmpty()) {
            scenarios = rows(records, columns, errors);
        }
        if (!errors.list.isEmpty()) {
            throw new CsvValidationException(errors.list);
        }

        Depot depot = depots.findBySourceDatasetAndSourceIdentifier(Mn01ImportService.DEPOT_DATASET, "MN01G")
                .orElseThrow(() -> new ConflictException("The MN01G depot is missing; run the MN01 import first."));
        String version = matrices.roadGraphVersion();
        String engine = matrices.routingEngine();

        List<BatchScenario> built = new ArrayList<>();
        TreeSet<String> allSnapshots = new TreeSet<>();
        for (Scenario s : scenarios) {
            BatchScenario b = build(s, depot);
            built.add(b);
            allSnapshots.addAll(b.sourceSnapshotIds());
        }
        String fileLabel = label != null && !label.isBlank()
                ? label.trim()
                : fileName != null && !fileName.isBlank() ? "CSV import " + fileName.trim() : "CSV import";
        if (fileLabel.length() > 120) {
            fileLabel = fileLabel.substring(0, 120);
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String sha = sha256(body);
        EvaluationBatch batch = EvaluationBatch.startCsvImport(fileLabel, blankToNull(fileName), sha, dataRows,
                new ArrayList<>(allSnapshots), version, engine, ExperimentService.environment(), built.size(),
                username, now);
        List<EvaluationRun> computed = new ArrayList<>();
        for (BatchScenario b : built) {
            EvaluationRun run = evaluations.computeBatchRun(b, ScenarioKind.CSV_IMPORT, batch.getId(), username);
            batch.recordRun(run.getStatus());
            computed.add(run);
        }
        batch.finish(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        String finalLabel = fileLabel;
        String shown = blankToNull(fileName) == null ? "-" : fileName.trim();
        return tx.execute(st -> {
            EvaluationBatch saved = batches.save(batch);
            runs.saveAll(computed);
            audit.record(AuditAction.EVALUATION_SCENARIOS_IMPORTED, AuditEntityType.EVALUATION_BATCH, saved.getId(),
                    saved.getFailedRunCount() > 0 ? AuditOutcome.FAILURE : AuditOutcome.SUCCESS,
                    finalLabel + ": file " + shown + " (" + dataRows + " rows, sha " + sha.substring(0, 12)
                            + "); scenarios " + saved.getScenarioCount() + ", completed "
                            + saved.getCompletedRunCount() + ", routing failed " + saved.getFailedRunCount());
            return saved;
        });
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- header

    private Map<String, Integer> header(List<List<String>> records, Errors errors) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        if (records.isEmpty()) {
            errors.add(1, null, "no data rows");
            return columns;
        }
        List<String> h = records.get(0);
        for (int i = 0; i < h.size(); i++) {
            String name = h.get(i).trim();
            if (!KNOWN.contains(name)) {
                errors.add(1, name, "unknown column " + name);
            } else if (columns.containsKey(name)) {
                errors.add(1, name, "duplicate column " + name);
            } else {
                columns.put(name, i);
            }
        }
        for (String r : REQUIRED) {
            if (!columns.containsKey(r)) {
                errors.add(1, r, "missing column " + r);
            }
        }
        boolean source = columns.containsKey("source_dataset") && columns.containsKey("source_identifier");
        if (!columns.containsKey("point_id") && !source) {
            errors.add(1, null, "missing point identity columns: point_id or source_dataset and source_identifier");
        }
        if (errors.list.isEmpty() && records.size() < 2) {
            errors.add(1, null, "no data rows");
        }
        return columns;
    }

    // ------------------------------------------------------------------ rows

    private static final class Scenario {
        final String key;
        final int firstRow;
        final List<Row> rows = new ArrayList<>();

        Scenario(String key, int firstRow) {
            this.key = key;
            this.firstRow = firstRow;
        }
    }

    private List<Scenario> rows(List<List<String>> records, Map<String, Integer> columns, Errors errors) {
        List<Row> clean = new ArrayList<>();
        Map<String, Optional<CollectionPoint>> pointCache = new HashMap<>();
        Map<String, Optional<WasteCategory>> categoryCache = new HashMap<>();
        int dataRows = records.size() - 1;
        for (int i = 1; i < records.size(); i++) {
            int rowNo = i + 1;
            if (i > MAX_ROWS) {
                errors.add(MAX_ROWS + 2, null, "more than " + MAX_ROWS + " data rows");
                break;
            }
            List<String> f = records.get(i);
            if (f.size() != records.get(0).size()) {
                errors.add(rowNo, null, "expected " + records.get(0).size() + " fields, found " + f.size());
                continue;
            }
            Row r = checkRow(rowNo, f, columns, errors, pointCache, categoryCache);
            if (r != null) {
                clean.add(r);
            }
        }
        if (dataRows < 1) {
            return List.of();
        }
        return crossChecks(clean, errors);
    }

    private static String cell(List<String> f, Map<String, Integer> columns, String name) {
        Integer idx = columns.get(name);
        return idx == null ? "" : f.get(idx).trim();
    }

    private Row checkRow(int rowNo, List<String> f, Map<String, Integer> columns, Errors errors,
            Map<String, Optional<CollectionPoint>> pointCache, Map<String, Optional<WasteCategory>> categoryCache) {
        int before = errors.list.size();
        String key = cell(f, columns, "scenario_key");
        if (key.isEmpty()) {
            errors.add(rowNo, "scenario_key", "required");
        } else if (!KEY.matcher(key).matches()) {
            errors.add(rowNo, "scenario_key", "must match [A-Za-z0-9._-]{1,80}");
        }
        int order = 0;
        String orderText = cell(f, columns, "input_order");
        if (orderText.isEmpty()) {
            errors.add(rowNo, "input_order", "required");
        } else if (!INTEGER.matcher(orderText).matches() || orderText.length() > 9 || Integer.parseInt(orderText) < 1) {
            errors.add(rowNo, "input_order", "must be an integer >= 1");
        } else {
            order = Integer.parseInt(orderText);
        }

        // identity
        String pointIdText = cell(f, columns, "point_id");
        String dataset = cell(f, columns, "source_dataset");
        String identifier = cell(f, columns, "source_identifier");
        boolean hasId = !pointIdText.isEmpty();
        boolean hasSourcePart = !dataset.isEmpty() || !identifier.isEmpty();
        CollectionPoint point = null;
        String ref = null;
        if (hasId && hasSourcePart) {
            errors.add(rowNo, null, "give either point_id or source_dataset/source_identifier, not both");
        } else if (hasId) {
            ref = pointIdText;
            try {
                UUID id = UUID.fromString(pointIdText);
                point = pointCache.computeIfAbsent("id:" + id, k -> points.findById(id)).orElse(null);
                if (point == null) {
                    errors.add(rowNo, "point_id", "point " + ref + " not found");
                }
            } catch (IllegalArgumentException e) {
                errors.add(rowNo, "point_id", "not a UUID");
            }
        } else if (!dataset.isEmpty() && !identifier.isEmpty()) {
            ref = dataset + "/" + identifier;
            point = pointCache.computeIfAbsent("src:" + ref,
                    k -> points.findBySourceDatasetAndSourceIdentifier(dataset, identifier)).orElse(null);
            if (point == null) {
                errors.add(rowNo, "source_identifier", "point " + ref + " not found");
            }
        } else if (hasSourcePart) {
            errors.add(rowNo, dataset.isEmpty() ? "source_dataset" : "source_identifier",
                    "source_dataset and source_identifier must both be given");
        } else {
            errors.add(rowNo, null, "give point_id or source_dataset/source_identifier");
        }
        if (point != null && point.getSourceOwnershipType() != null
                && !point.getSourceOwnershipType().startsWith(CollectionPoint.DSNY_TYPE_PREFIX)) {
            errors.add(rowNo, hasId ? "point_id" : "source_identifier",
                    "point " + ref + " is not a DSNY basket (" + point.getSourceOwnershipType() + ")");
            point = null;
        }

        // coordinates
        Double lon = coordinate(f, columns, "longitude", 180, rowNo, errors);
        Double lat = coordinate(f, columns, "latitude", 90, rowNo, errors);
        if (lon != null && lat != null && !area.contains(lon, lat)) {
            errors.add(rowNo, "longitude", "outside the service area (longitude and latitude may be swapped)");
            lon = null;
        }
        if (point != null && lon != null && lat != null) {
            if (Math.abs(point.getLongitude() - lon) > 1e-6) {
                errors.add(rowNo, "longitude", "differs from the point's stored coordinate " + point.getLongitude());
            }
            if (Math.abs(point.getLatitude() - lat) > 1e-6) {
                errors.add(rowNo, "latitude", "differs from the point's stored coordinate " + point.getLatitude());
            }
        }

        // category
        WasteCategory category = null;
        String catName = cell(f, columns, "category");
        if (catName.isEmpty()) {
            errors.add(rowNo, "category", "required");
        } else {
            category = categoryCache.computeIfAbsent(catName.toLowerCase(Locale.ROOT),
                    k -> categories.findByNameIgnoreCase(catName)).orElse(null);
            if (category == null) {
                errors.add(rowNo, "category", "category " + catName + " not found");
            } else if (!category.isActive()) {
                errors.add(rowNo, "category", "category " + catName + " is inactive");
                category = null;
            }
        }

        BigDecimal demand = kilograms(cell(f, columns, "demand_kg"), "demand_kg", rowNo, errors);
        BigDecimal capacity = kilograms(cell(f, columns, "vehicle_capacity_kg"), "vehicle_capacity_kg", rowNo, errors);

        String profile = cell(f, columns, "demand_profile");
        if (profile.length() > 40) {
            errors.add(rowNo, "demand_profile", "must be at most 40 characters");
        }
        Long seed = null;
        String seedText = cell(f, columns, "seed");
        if (!seedText.isEmpty()) {
            try {
                if (!INTEGER.matcher(seedText).matches()) {
                    throw new NumberFormatException();
                }
                seed = Long.parseLong(seedText);
            } catch (NumberFormatException e) {
                errors.add(rowNo, "seed", "must be an integer >= 0");
            }
        }
        if (errors.list.size() > before) {
            return null;
        }
        return new Row(rowNo, key, order, point, ref, lon, lat, category, demand, capacity,
                profile.isEmpty() ? null : profile, seed);
    }

    private static Double coordinate(List<String> f, Map<String, Integer> columns, String name, double limit,
            int rowNo, Errors errors) {
        String t = cell(f, columns, name);
        if (t.isEmpty()) {
            errors.add(rowNo, name, "required");
            return null;
        }
        if (!DECIMAL.matcher(t).matches()) {
            errors.add(rowNo, name, "must be a number");
            return null;
        }
        double v = Double.parseDouble(t);
        if (Math.abs(v) > limit) {
            errors.add(rowNo, name, "must be between -" + (int) limit + " and " + (int) limit);
            return null;
        }
        return v;
    }

    private static BigDecimal kilograms(String t, String name, int rowNo, Errors errors) {
        if (t.isEmpty()) {
            errors.add(rowNo, name, "required");
            return null;
        }
        if (!DECIMAL.matcher(t).matches()) {
            errors.add(rowNo, name, "must be a number");
            return null;
        }
        BigDecimal v = new BigDecimal(t);
        if (v.signum() <= 0) {
            errors.add(rowNo, name, "must be > 0");
            return null;
        }
        if (v.compareTo(MAX_KG) > 0) {
            errors.add(rowNo, name, "must be at most 100000");
            return null;
        }
        if (v.stripTrailingZeros().scale() > 2) {
            errors.add(rowNo, name, "must have at most 2 decimals");
            return null;
        }
        return v.setScale(2);
    }

    // ------------------------------------------------------------ cross-row

    private List<Scenario> crossChecks(List<Row> clean, Errors errors) {
        Map<String, Scenario> byKey = new LinkedHashMap<>();
        for (Row r : clean) {
            byKey.computeIfAbsent(r.key(), k -> new Scenario(k, r.row())).rows.add(r);
        }
        List<Scenario> result = new ArrayList<>();
        int count = 0;
        for (Scenario s : byKey.values()) {
            count++;
            if (count == MAX_SCENARIOS + 1) {
                errors.add(s.firstRow, "scenario_key", "more than " + MAX_SCENARIOS + " scenarios");
            }
            Scenario kept = new Scenario(s.key, s.firstRow);
            Map<Integer, UUID> orderToPoint = new HashMap<>();
            Map<UUID, Integer> pointToOrder = new HashMap<>();
            Map<Integer, Set<String>> orderCategories = new HashMap<>();
            Row first = s.rows.get(0);
            for (Row r : s.rows) {
                boolean ok = true;
                UUID pid = r.point().getId();
                UUID known = orderToPoint.get(r.inputOrder());
                if (known != null && !known.equals(pid)) {
                    errors.add(r.row(), null, "input order " + r.inputOrder() + " refers to two different points");
                    ok = false;
                } else if (known == null && pointToOrder.containsKey(pid)
                        && pointToOrder.get(pid) != r.inputOrder()) {
                    errors.add(r.row(), null, "point " + r.ref() + " already listed at input order "
                            + pointToOrder.get(pid));
                    ok = false;
                }
                if (r.capacity().compareTo(first.capacity()) != 0) {
                    errors.add(r.row(), "vehicle_capacity_kg", "vehicle_capacity_kg differs within scenario " + s.key);
                    ok = false;
                }
                if (!Objects.equals(r.profile(), first.profile())) {
                    errors.add(r.row(), "demand_profile", "demand_profile differs within scenario " + s.key);
                    ok = false;
                }
                if (!Objects.equals(r.seed(), first.seed())) {
                    errors.add(r.row(), "seed", "seed differs within scenario " + s.key);
                    ok = false;
                }
                if (ok) {
                    Set<String> cats = orderCategories.computeIfAbsent(r.inputOrder(), k -> new HashSet<>());
                    if (!cats.add(r.category().getName().toLowerCase(Locale.ROOT))) {
                        errors.add(r.row(), null, "category " + r.category().getName()
                                + " listed twice for input order " + r.inputOrder());
                        ok = false;
                    }
                }
                if (ok) {
                    orderToPoint.putIfAbsent(r.inputOrder(), pid);
                    pointToOrder.putIfAbsent(pid, r.inputOrder());
                    kept.rows.add(r);
                }
            }
            int n = orderToPoint.size();
            if (n > MAX_POINTS) {
                errors.add(s.firstRow, null, "scenario " + s.key + " has more than " + MAX_POINTS + " points");
            }
            boolean contiguous = true;
            for (int k = 1; k <= n; k++) {
                if (!orderToPoint.containsKey(k)) {
                    contiguous = false;
                    break;
                }
            }
            if (!contiguous) {
                errors.add(s.firstRow, "input_order",
                        "input orders of scenario " + s.key + " must be 1.." + n + " without gaps");
            }
            result.add(kept);
        }
        return result;
    }

    // ------------------------------------------------------------ evaluation

    private BatchScenario build(Scenario s, Depot depot) {
        Map<Integer, List<Row>> byOrder = new TreeMap<>();
        for (Row r : s.rows) {
            byOrder.computeIfAbsent(r.inputOrder(), k -> new ArrayList<>()).add(r);
        }
        TreeSet<String> datasets = new TreeSet<>();
        boolean userEntered = false;
        if (depot.getSourceDataset() == null) {
            userEntered = true;
        } else {
            datasets.add(depot.getSourceDataset());
        }
        List<FrozenStop> stops = new ArrayList<>();
        for (Map.Entry<Integer, List<Row>> e : byOrder.entrySet()) {
            List<Row> rows = e.getValue();
            Row r = rows.get(0);
            CollectionPoint p = r.point();
            if (p.getSourceDataset() == null) {
                userEntered = true;
            } else {
                datasets.add(p.getSourceDataset());
            }
            BigDecimal total = BigDecimal.ZERO;
            List<Row> sorted = new ArrayList<>(rows);
            sorted.sort(Comparator.comparing(x -> x.category().getName().toLowerCase(Locale.ROOT)));
            List<String> parts = new ArrayList<>();
            for (Row x : sorted) {
                total = total.add(x.demand());
                parts.add(x.category().getName() + "=" + x.demand().toPlainString());
            }
            String breakdown = String.join("; ", parts);
            if (breakdown.length() > 500) {
                breakdown = breakdown.substring(0, 500);
            }
            stops.add(new FrozenStop(e.getKey(), p.getId(), p.getSourceDataset(), p.getSourceIdentifier(),
                    p.getName(), r.lon(), r.lat(), total, breakdown));
        }
        List<String> ids = new ArrayList<>();
        for (String ds : datasets) {
            ids.add(snapshots.findBySourceDataset(ds).map(x -> x.getSnapshotId()).orElse(ds + ":unknown"));
        }
        if (userEntered) {
            ids.add("USER_ENTERED");
        }
        Row first = s.rows.get(0);
        return new BatchScenario(s.key, first.profile(), first.seed(), null,
                new FrozenDepot(depot.getName(), depot.getLongitude(), depot.getLatitude(),
                        depot.getSourceIdentifier()),
                stops, first.capacity(), ids);
    }
}
