package com.ledgora.customer.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CBS-grade Customer Tax Profile for RBI compliance.
 * Stores PAN, TAN, GST and other tax identification details.
 */
@Entity
@Table(name = "customer_tax_profile", indexes = {
    @Index(name = "idx_ctp_customer", columnList = "customer_master_id"),
    @Index(name = "idx_ctp_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ctp_pan", columnList = "pan_number")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerTaxProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_master_id", nullable = false)
    private CustomerMaster customerMaster;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "tan_number", length = 20)
    private String tanNumber;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "tax_residency_status", length = 20)
    @Builder.Default
    private String taxResidencyStatus = "RESIDENT";

    @Column(name = "tax_deduction_flag")
    @Builder.Default
    private Boolean taxDeductionFlag = true;

    @Column(name = "exemption_code", length = 20)
    private String exemptionCode;

    @Column(name = "exemption_valid_till")
    private LocalDate exemptionValidTill;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
