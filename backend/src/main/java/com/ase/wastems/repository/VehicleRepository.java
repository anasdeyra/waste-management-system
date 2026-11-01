package com.ase.wastems.repository;

import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link Vehicle}. */
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findAllByOrderByRegistrationAsc();

    List<Vehicle> findByStatusOrderByRegistrationAsc(ResourceStatus status);

    long countByStatus(ResourceStatus status);

    boolean existsByRegistration(String registration);

    boolean existsByRegistrationAndIdNot(String registration, UUID id);

    Optional<Vehicle> findByRegistration(String registration);
}
