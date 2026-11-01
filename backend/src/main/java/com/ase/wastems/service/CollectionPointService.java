package com.ase.wastems.service;

import com.ase.wastems.config.ServiceAreaProperties;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.CollectionPointRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules of collection points and their demand entries. */
@Service
public class CollectionPointService {

    private final CollectionPointRepository points;
    private final WasteCategoryService categories;
    private final ServiceAreaProperties area;
    private final AuditService audit;

    public CollectionPointService(
            CollectionPointRepository points,
            WasteCategoryService categories,
            ServiceAreaProperties area,
            AuditService audit) {
        this.audit = audit;
        this.points = points;
        this.categories = categories;
        this.area = area;
    }

    @Transactional(readOnly = true)
    public List<CollectionPoint> search(CollectionPointStatus status, UUID categoryId) {
        return search(status, categoryId, null);
    }

    /** {@code eligible} null means no filter; otherwise keeps points whose D-019 eligibility matches. */
    @Transactional(readOnly = true)
    public List<CollectionPoint> search(
            CollectionPointStatus status, UUID categoryId, Boolean eligible) {
        return points.search(status, categoryId).stream()
                .filter(p -> eligible == null || p.isOptimizationEligible() == eligible)
                .toList();
    }

    @Transactional(readOnly = true)
    public CollectionPoint get(UUID id) {
        return load(id);
    }

    /**
     * Points that may be planned (D-019): active, and either user-entered or of a {@code DSNY }
     * basket type. Used by tour drafts (S4) and evaluation scenarios (S5); inactive points are
     * never eligible (US-12).
     */
    @Transactional(readOnly = true)
    public List<CollectionPoint> findEligibleForPlanning() {
        return search(CollectionPointStatus.ACTIVE, null).stream()
                .filter(CollectionPoint::isOptimizationEligible)
                .toList();
    }

    @Transactional
    public CollectionPoint create(
            String name, double longitude, double latitude, List<DemandInput> demands) {
        requireInServiceArea(longitude, latitude);
        if (demands == null || demands.isEmpty()) {
            throw new BusinessRuleException("A collection point must have at least one demand entry.");
        }
        Set<UUID> seen = new HashSet<>();
        for (DemandInput d : demands) {
            if (!seen.add(d.categoryId())) {
                throw new ConflictException("Duplicate waste category in demand entries");
            }
        }
        CollectionPoint p = new CollectionPoint(name.trim(), longitude, latitude, DataOrigin.USER_ENTERED);
        for (DemandInput d : demands) {
            WasteCategory c = categories.requireActive(d.categoryId());
            p.addDemand(new DemandEntry(c, d.quantityKg(), DataOrigin.USER_ENTERED));
        }
        CollectionPoint saved = points.save(p);
        audit.record(
                AuditAction.POINT_CREATED,
                AuditEntityType.COLLECTION_POINT,
                saved.getId(),
                saved.getName()
                        + " at "
                        + longitude
                        + ", "
                        + latitude
                        + "; "
                        + demands.size()
                        + " demand entries");
        return saved;
    }

    @Transactional
    public CollectionPoint update(UUID id, String name, double longitude, double latitude) {
        CollectionPoint p = load(id);
        requireInServiceArea(longitude, latitude);
        p.setName(name.trim());
        p.setLongitude(longitude);
        p.setLatitude(latitude);
        audit.record(
                AuditAction.POINT_UPDATED,
                AuditEntityType.COLLECTION_POINT,
                p.getId(),
                p.getName() + " at " + longitude + ", " + latitude);
        return p;
    }

    @Transactional
    public CollectionPoint setStatus(UUID id, CollectionPointStatus status) {
        CollectionPoint p = load(id);
        p.setStatus(status);
        audit.record(
                AuditAction.POINT_STATUS_CHANGED,
                AuditEntityType.COLLECTION_POINT,
                p.getId(),
                status.name());
        return p;
    }

    @Transactional
    public DemandEntry addDemand(UUID pointId, UUID categoryId, BigDecimal quantityKg) {
        CollectionPoint p = load(pointId);
        WasteCategory c = categories.requireActive(categoryId);
        requireCategoryFree(p, c, null);
        DemandEntry d = new DemandEntry(c, quantityKg, DataOrigin.USER_ENTERED);
        p.addDemand(d);
        // p is managed: flushing cascades persist to d, which receives its id (save() would merge a copy)
        points.flush();
        recordDemand(AuditAction.DEMAND_ADDED, p, d);
        return d;
    }

    @Transactional
    public DemandEntry updateDemand(
            UUID pointId, UUID demandId, UUID categoryId, BigDecimal quantityKg) {
        CollectionPoint p = load(pointId);
        DemandEntry d = findDemand(p, demandId);
        if (!d.getCategory().getId().equals(categoryId)) {
            WasteCategory c = categories.requireActive(categoryId);
            requireCategoryFree(p, c, d);
            d.setCategory(c);
        }
        d.setQuantityKg(quantityKg);
        d.setOrigin(DataOrigin.USER_ENTERED);
        points.flush();
        recordDemand(AuditAction.DEMAND_UPDATED, p, d);
        return d;
    }

    @Transactional
    public void removeDemand(UUID pointId, UUID demandId) {
        CollectionPoint p = load(pointId);
        DemandEntry d = findDemand(p, demandId);
        if (p.getDemands().size() <= 1) {
            throw new BusinessRuleException("A collection point must keep at least one demand entry.");
        }
        String categoryName = d.getCategory().getName();
        p.removeDemand(d);
        audit.record(
                AuditAction.DEMAND_REMOVED,
                AuditEntityType.COLLECTION_POINT,
                p.getId(),
                categoryName + " (entry " + demandId + ")");
    }

    private void recordDemand(AuditAction action, CollectionPoint p, DemandEntry d) {
        audit.record(
                action,
                AuditEntityType.COLLECTION_POINT,
                p.getId(),
                d.getCategory().getName()
                        + " "
                        + d.getQuantityKg().toPlainString()
                        + " kg (entry "
                        + d.getId()
                        + ")");
    }

    private CollectionPoint load(UUID id) {
        return points
                .findWithDemandsById(id)
                .orElseThrow(() -> new NotFoundException("Collection point not found: " + id));
    }

    private static DemandEntry findDemand(CollectionPoint p, UUID demandId) {
        return p.getDemands().stream()
                .filter(d -> d.getId().equals(demandId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Demand entry not found: " + demandId));
    }

    private static void requireCategoryFree(CollectionPoint p, WasteCategory c, DemandEntry except) {
        boolean taken =
                p.getDemands().stream()
                        .anyMatch(e -> e != except && e.getCategory().getId().equals(c.getId()));
        if (taken) {
            throw new ConflictException(
                    "Waste category already has a demand entry on this point: " + c.getName());
        }
    }

    private void requireInServiceArea(double longitude, double latitude) {
        if (!area.contains(longitude, latitude)) {
            throw new BusinessRuleException(
                    "Coordinates (longitude "
                            + longitude
                            + ", latitude "
                            + latitude
                            + ") are outside the NYC service area. Check that longitude and latitude"
                            + " are not swapped.");
        }
    }
}
