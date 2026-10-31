package com.ase.wastems;

import static org.assertj.core.api.Assertions.assertThat;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Verifies that the application context starts and that the demo data is loaded. */
@SpringBootTest
@Transactional
class WasteMsApplicationTests {

    @Autowired
    private CollectionPointRepository collectionPointRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private WasteCategoryRepository wasteCategoryRepository;

    @Test
    @DisplayName("The demo data set is loaded on start-up")
    void demoDataIsLoaded() {
        assertThat(wasteCategoryRepository.count()).isEqualTo(5);
        assertThat(collectionPointRepository.count()).isEqualTo(12);
        assertThat(vehicleRepository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("A collection point keeps its waste category")
    void collectionPointReferencesItsCategory() {
        CollectionPoint point =
                collectionPointRepository.findAllByOrderByNameAsc().stream()
                        .findFirst()
                        .orElseThrow();

        assertThat(point.getWasteCategory()).isNotNull();
        assertThat(point.getStatus()).isEqualTo(CollectionPointStatus.ACTIVE);
    }

    @Test
    @DisplayName("The fleet exposes at least one available vehicle")
    void fleetHasAvailableVehicle() {
        assertThat(vehicleRepository.findByStatus(com.ase.wastems.domain.VehicleStatus.AVAILABLE))
                .isNotEmpty();
    }
}
