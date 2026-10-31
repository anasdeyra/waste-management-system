package com.ase.wastems.repository;

import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.domain.VehicleStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access to {@link Vehicle} records. */
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByStatus(VehicleStatus status);

    List<Vehicle> findAllByOrderByPlateAsc();
}
