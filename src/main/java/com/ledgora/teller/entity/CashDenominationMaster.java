package com.ledgora.teller.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * Cash Denomination Master — defines valid Indian currency denominations. Seeded at startup:
 * ₹2000, ₹500, ₹200, ₹100, ₹50, ₹20, ₹10. Immutable reference data.
 */
@Entity
@Table(
        name = "cash_denomination_masters",
        indexes = {
            @Index(
                    name = "idx_denom_value",
                    columnList = "denomination_value",
                    unique = true)
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashDenominationMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Face value of the denomination (e.g., 2000, 500, 100). */
    @Column(name = "denomination_value", precision = 19, scale = 4, nullable = false, unique = true)
    private BigDecimal denominationValue;

    /** Display label (e.g., "₹2000", "₹500"). */
    @Column(name = "label", length = 20, nullable = false)
    private String label;

    /** Whether this denomination is currently in circulation. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Sort order for UI display (highest denomination first). */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
