package com.ase.wastems.config;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a small map sample in the {@code dev} profile: the MN01G depot and five DSNY baskets
 * with real locations copied verbatim from the included NYC Open Data snapshots, and synthetic
 * demand (two demo categories). Idempotent: rows are keyed by source dataset + identifier.
 *
 * <p>The full 449-row MN01 import is S3 (US-33 groundwork) and upserts these rows by their
 * source key instead of duplicating them (see Mn01ImportService). Never active outside {@code dev}.
 */
@Component
@Profile("dev")
@Order(20)
public class DevMapDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DevMapDataSeeder.class);

    static final String BASKET_DATASET = "8znf-7b2c";
    static final String DEPOT_DATASET = "xw3j-2yxf";

    private final WasteCategoryRepository categories;
    private final DepotRepository depots;
    private final CollectionPointRepository points;

    public DevMapDataSeeder(
            WasteCategoryRepository categories,
            DepotRepository depots,
            CollectionPointRepository points) {
        this.categories = categories;
        this.depots = depots;
        this.points = points;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        WasteCategory litter =
                category("Mixed litter", "Street litter-basket refuse (synthetic demo category)");
        WasteCategory recyclables =
                category(
                        "Recyclables",
                        "Metal, glass, plastic and paper (synthetic demo category)");

        if (depots.findBySourceDatasetAndSourceIdentifier(DEPOT_DATASET, "MN01G").isEmpty()) {
            depots.save(
                    new Depot(
                            "MN01G Garage, 353 Spring St",
                            -74.0104183829902,
                            40.7255225360961,
                            DataOrigin.IMPORTED_REAL,
                            DEPOT_DATASET,
                            "MN01G"));
        }

        basket("10130067", "CENTRE STREET & WORTH STREET", "DSNY PSR", -74.00212207, 40.71528035,
                CollectionPointStatus.ACTIVE, Map.of(litter, "30", recyclables, "12"));
        basket("10130002", "WEST BROADWAY & THOMAS STREET", "DSNY Wire", -74.00821676, 40.71706895,
                CollectionPointStatus.ACTIVE, Map.of(litter, "18"));
        basket("10110232", "WARREN STREET & GREENWICH STREET", "DSNY PSR", -74.0115043, 40.71560317,
                CollectionPointStatus.ACTIVE, Map.of(litter, "25", recyclables, "8"));
        basket("10130023", "VARICK STREET & FRANKLIN STREET", "DSNY PSR", -74.0070756, 40.71926549,
                CollectionPointStatus.ACTIVE, Map.of(litter, "22"));
        basket("10130345", "BROADWAY & THOMAS STREET", "DSNY PSR", -74.00505623, 40.71595132,
                CollectionPointStatus.INACTIVE, Map.of(litter, "15"));

        LOG.info(
                "Dev map seed present: {} categories, {} depots, {} collection points",
                categories.count(),
                depots.count(),
                points.count());
    }

    private WasteCategory category(String name, String description) {
        return categories
                .findByNameIgnoreCase(name)
                .orElseGet(() -> categories.save(new WasteCategory(name, description)));
    }

    private void basket(
            String basketId,
            String name,
            String type,
            double longitude,
            double latitude,
            CollectionPointStatus status,
            Map<WasteCategory, String> demands) {
        if (points.existsBySourceDatasetAndSourceIdentifier(BASKET_DATASET, basketId)) {
            return;
        }
        CollectionPoint p = new CollectionPoint(name, longitude, latitude, DataOrigin.IMPORTED_REAL);
        p.setSource(BASKET_DATASET, basketId, type);
        p.setStatus(status);
        demands.forEach(
                (c, kg) -> p.addDemand(new DemandEntry(c, new BigDecimal(kg), DataOrigin.SYNTHETIC)));
        points.save(p);
    }
}
