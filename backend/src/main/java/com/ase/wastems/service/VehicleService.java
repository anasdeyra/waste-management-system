package com.ase.wastems.service;

import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules of vehicles: unique normalized registration, positive capacity, availability. */
@Service
public class VehicleService {

    private final VehicleRepository vehicles;

    private final AuditService audit;

    public VehicleService(VehicleRepository vehicles, AuditService audit) {
        this.audit = audit;
        this.vehicles = vehicles;
    }

    static String normalizeRegistration(String raw) {
        return raw.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> list(ResourceStatus status) {
        return status == null
                ? vehicles.findAllByOrderByRegistrationAsc()
                : vehicles.findByStatusOrderByRegistrationAsc(status);
    }

    @Transactional(readOnly = true)
    public Vehicle get(UUID id) {
        return vehicles.findById(id).orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));
    }

    @Transactional
    public Vehicle create(String registration, BigDecimal capacityKg) {
        requirePositive(capacityKg);
        String reg = normalizeRegistration(registration);
        if (vehicles.existsByRegistration(reg)) {
            throw new ConflictException("Vehicle registration already exists: " + reg);
        }
        Vehicle saved = vehicles.save(new Vehicle(reg, capacityKg, DataOrigin.USER_ENTERED));
        audit.record(AuditAction.VEHICLE_CREATED, AuditEntityType.VEHICLE, saved.getId(), describe(saved));
        return saved;
    }

    /** Edits registration and capacity; the identifier never changes (US-13). */
    @Transactional
    public Vehicle update(UUID id, String registration, BigDecimal capacityKg) {
        requirePositive(capacityKg);
        Vehicle v = get(id);
        String reg = normalizeRegistration(registration);
        if (vehicles.existsByRegistrationAndIdNot(reg, id)) {
            throw new ConflictException("Vehicle registration already exists: " + reg);
        }
        v.setRegistration(reg);
        v.setCapacityKg(capacityKg);
        audit.record(AuditAction.VEHICLE_UPDATED, AuditEntityType.VEHICLE, v.getId(), describe(v));
        return v;
    }

    @Transactional
    public Vehicle setStatus(UUID id, ResourceStatus status) {
        Vehicle v = get(id);
        v.setStatus(status);
        audit.record(
                AuditAction.VEHICLE_STATUS_CHANGED, AuditEntityType.VEHICLE, v.getId(), status.name());
        return v;
    }

    /** Vehicles that may be assigned to a new tour (US-14); S4 tour drafts use it. */
    @Transactional(readOnly = true)
    public List<Vehicle> findUsable() {
        return list(ResourceStatus.AVAILABLE);
    }

    private static String describe(Vehicle v) {
        return v.getRegistration() + ", capacity " + v.getCapacityKg().toPlainString() + " kg";
    }

    private static void requirePositive(BigDecimal capacityKg) {
        if (capacityKg == null || capacityKg.signum() <= 0) {
            throw new BusinessRuleException("Vehicle capacity must be a positive number of kilograms.");
        }
    }
}
