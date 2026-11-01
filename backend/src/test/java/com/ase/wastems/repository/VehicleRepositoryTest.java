package com.ase.wastems.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.Vehicle;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Persistence tests of {@link VehicleRepository}. */
@DataJpaTest
@ActiveProfiles("test")
class VehicleRepositoryTest {

    @Autowired private VehicleRepository repository;

    @Test
    void roundTripKeepsFields() {
        Vehicle saved =
                repository.saveAndFlush(
                        new Vehicle("NY-1", new BigDecimal("9000.00"), DataOrigin.USER_ENTERED));
        Vehicle found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("NY-1", found.getRegistration());
        assertEquals(0, found.getCapacityKg().compareTo(new BigDecimal("9000")));
        assertEquals(ResourceStatus.AVAILABLE, found.getStatus());
        assertEquals(DataOrigin.USER_ENTERED, found.getOrigin());
    }

    @Test
    void duplicateRegistrationIsRejected() {
        repository.saveAndFlush(new Vehicle("NY-1", BigDecimal.TEN, DataOrigin.USER_ENTERED));
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        repository.saveAndFlush(
                                new Vehicle("NY-1", BigDecimal.ONE, DataOrigin.USER_ENTERED)));
    }

    @Test
    void filtersByStatus() {
        repository.saveAndFlush(new Vehicle("A-1", BigDecimal.TEN, DataOrigin.USER_ENTERED));
        Vehicle b = new Vehicle("B-1", BigDecimal.TEN, DataOrigin.USER_ENTERED);
        b.setStatus(ResourceStatus.UNAVAILABLE);
        repository.saveAndFlush(b);
        List<Vehicle> found = repository.findByStatusOrderByRegistrationAsc(ResourceStatus.UNAVAILABLE);
        assertEquals(1, found.size());
        assertEquals("B-1", found.get(0).getRegistration());
    }
}
