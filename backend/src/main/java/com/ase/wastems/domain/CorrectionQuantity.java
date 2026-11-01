package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * One category quantity retained in an {@link OutcomeCorrection}: the category id, its name at the
 * time of the correction and the kilograms (US-31).
 */
@Embeddable
public class CorrectionQuantity {

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "category_name", nullable = false, length = 80)
    private String categoryName;

    @Column(name = "quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityKg;

    protected CorrectionQuantity() {}

    public CorrectionQuantity(UUID categoryId, String categoryName, BigDecimal quantityKg) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.quantityKg = quantityKg;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getQuantityKg() {
        return quantityKg;
    }
}
