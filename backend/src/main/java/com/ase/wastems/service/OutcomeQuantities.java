package com.ase.wastems.service;

import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Category rules shared by outcome recording (S7) and outcome corrections (S8). */
final class OutcomeQuantities {

    private OutcomeQuantities() {}

    /**
     * Resolves the requested quantities: every category is looked up first (404), then duplicates
     * are refused (422), then inactive categories that are not in {@code allowedInactive} (422).
     * The result keeps the request order.
     */
    static Map<WasteCategory, BigDecimal> resolve(
            WasteCategoryRepository categories,
            List<StopOutcomeCommand.QuantityInput> inputs,
            Set<UUID> allowedInactive) {
        Map<WasteCategory, BigDecimal> resolved = new LinkedHashMap<>();
        Set<UUID> seen = new HashSet<>();
        List<WasteCategory> found = new ArrayList<>();
        for (StopOutcomeCommand.QuantityInput q : inputs) {
            WasteCategory c = categories.findById(q.categoryId()).orElseThrow(
                    () -> new NotFoundException("Waste category not found: " + q.categoryId()));
            found.add(c);
        }
        for (WasteCategory c : found) {
            if (!seen.add(c.getId())) {
                throw new BusinessRuleException("Each waste category may appear only once.");
            }
        }
        for (int i = 0; i < found.size(); i++) {
            WasteCategory c = found.get(i);
            if (!c.isActive() && !allowedInactive.contains(c.getId())) {
                throw new BusinessRuleException("Waste category " + c.getName() + " is inactive.");
            }
            resolved.put(c, inputs.get(i).quantityKg());
        }
        return resolved;
    }
}
