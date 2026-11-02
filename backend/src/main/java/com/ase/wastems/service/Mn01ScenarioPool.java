package com.ase.wastems.service;

import com.ase.wastems.config.ServiceAreaProperties;
import com.ase.wastems.domain.CollectionPoint;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Pool of real DSNY baskets of the committed MN01 snapshot, the sampling frame of the seeded
 * experiment generator mn01-v1 (D-050). Read from the same classpath bytes as the importer.
 */
@Component
public class Mn01ScenarioPool {

    private final ServiceAreaProperties area;
    private List<PoolBasket> baskets;
    private String basketSnapshotId;
    private String depotSnapshotId;

    public Mn01ScenarioPool(ServiceAreaProperties area) {
        this.area = area;
    }

    public List<PoolBasket> baskets() {
        load();
        return baskets;
    }

    public String basketSnapshotId() {
        load();
        return basketSnapshotId;
    }

    public String depotSnapshotId() {
        load();
        return depotSnapshotId;
    }

    private synchronized void load() {
        if (baskets != null) {
            return;
        }
        byte[] basketBytes = read(Mn01ImportService.BASKET_FILE);
        byte[] depotBytes = read(Mn01ImportService.DEPOT_FILE);
        List<Map<String, String>> basketRows = SimpleCsv.parse(new String(basketBytes, StandardCharsets.UTF_8));
        List<Map<String, String>> depotRows = SimpleCsv.parse(new String(depotBytes, StandardCharsets.UTF_8));
        List<PoolBasket> pool = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, String> row : basketRows) {
            String id = value(row, "basketid");
            String type = value(row, "baskettype");
            if (id.isEmpty() || type.isEmpty() || !seen.add(id)) {
                continue;
            }
            try {
                double lon = Double.parseDouble(value(row, "longitude"));
                double lat = Double.parseDouble(value(row, "latitude"));
                if (!area.contains(lon, lat) || !type.startsWith(CollectionPoint.DSNY_TYPE_PREFIX)) {
                    continue;
                }
                pool.add(new PoolBasket(id,
                        Mn01ImportService.basketName(value(row, "streetname1"), value(row, "streetname2"), id),
                        lon, lat));
            } catch (NumberFormatException e) {
                // the importer skips unparsable rows too
            }
        }
        pool.sort(Comparator.comparing(PoolBasket::basketId));
        basketSnapshotId = snapshotId(Mn01ImportService.BASKET_DATASET, basketRows.size(), basketBytes);
        depotSnapshotId = snapshotId(Mn01ImportService.DEPOT_DATASET, depotRows.size(), depotBytes);
        baskets = List.copyOf(pool);
    }

    private static String snapshotId(String dataset, int rows, byte[] bytes) {
        try {
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return dataset + ":" + rows + ":" + sha.substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read source snapshot " + path, e);
        }
    }

    private static String value(Map<String, String> row, String column) {
        String v = row.get(column);
        return v == null ? "" : v.trim();
    }
}
