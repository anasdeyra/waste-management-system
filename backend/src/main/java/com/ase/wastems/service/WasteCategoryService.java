package com.ase.wastems.service;

import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules of waste categories. Categories are deactivated, never deleted. */
@Service
public class WasteCategoryService {

    private final WasteCategoryRepository categories;

    private final AuditService audit;

    public WasteCategoryService(WasteCategoryRepository categories, AuditService audit) {
        this.audit = audit;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<WasteCategory> list(Boolean activeOnly) {
        return Boolean.TRUE.equals(activeOnly)
                ? categories.findByActiveTrueOrderByNameAsc()
                : categories.findAllByOrderByNameAsc();
    }

    /** Lightweight active-categories query used by demand validation. */
    @Transactional(readOnly = true)
    public List<WasteCategory> findActive() {
        return categories.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public WasteCategory get(UUID id) {
        return categories
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Waste category not found: " + id));
    }

    @Transactional(readOnly = true)
    public WasteCategory requireActive(UUID id) {
        WasteCategory c = get(id);
        if (!c.isActive()) {
            throw new BusinessRuleException("Waste category is inactive: " + c.getName());
        }
        return c;
    }

    @Transactional
    public WasteCategory create(String name, String description) {
        String n = name.trim();
        if (categories.existsByNameIgnoreCase(n)) {
            throw new ConflictException("Waste category already exists: " + n);
        }
        WasteCategory saved = categories.save(new WasteCategory(n, normalizeDescription(description)));
        audit.record(
                AuditAction.CATEGORY_CREATED, AuditEntityType.WASTE_CATEGORY, saved.getId(), "name " + n);
        return saved;
    }

    @Transactional
    public WasteCategory update(UUID id, String name, String description) {
        WasteCategory c = get(id);
        String n = name.trim();
        if (categories.existsByNameIgnoreCaseAndIdNot(n, id)) {
            throw new ConflictException("Waste category already exists: " + n);
        }
        c.setName(n);
        c.setDescription(normalizeDescription(description));
        audit.record(
                AuditAction.CATEGORY_UPDATED, AuditEntityType.WASTE_CATEGORY, c.getId(), "name " + n);
        return c;
    }

    @Transactional
    public WasteCategory setActive(UUID id, boolean active) {
        WasteCategory c = get(id);
        c.setActive(active);
        audit.record(
                AuditAction.CATEGORY_STATUS_CHANGED,
                AuditEntityType.WASTE_CATEGORY,
                c.getId(),
                active ? "activated" : "deactivated");
        return c;
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String d = description.trim();
        return d.isEmpty() ? null : d;
    }
}
