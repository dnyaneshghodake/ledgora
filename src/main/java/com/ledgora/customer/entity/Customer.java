package com.ledgora.customer.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PART 1: Customer entity for KYC and account ownership.
 * A customer can own multiple accounts.
 * Multi-tenant aware.
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_national_id", columnList = "national_id", unique = true),
    @Index(name = "idx_customer_email", columnList = "email"),
    @Index(name = "idx_customer_tenant", columnList = "tenant_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "national_id", length = 50, unique = true)
    private String nationalId;

    @Column(name = "kyc_status", length = 20, nullable = false)
    @Builder.Default
    private String kycStatus = "PENDING";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
