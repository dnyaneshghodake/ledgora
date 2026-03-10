package com.ledgora.clearing.entity;

import com.ledgora.branch.entity.Branch;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration table mapping each branch to its inter-branch clearing GL code.
 *
 * <p>CBS Standard: Each branch MUST have a dedicated clearing GL. No global clearing account
 * allowed. Clearing GL resolution is configuration-driven, not hardcoded.
 *
 * <p>RBI Requirement: Branch-level trial balance isolation — each branch must independently balance
 * its books using its own clearing GL.
 */
@Entity
@Table(
        name = "branch_gl_mappings",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_branch_gl_tenant_branch",
                    columnNames = {"tenant_id", "branch_id"})
        },
        indexes = {
            @Index(name = "idx_bgl_tenant", columnList = "tenant_id"),
            @Index(name = "idx_bgl_branch", columnList = "branch_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchGlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /** The clearing GL code for this branch (e.g., "2910"). */
    @Column(name = "clearing_gl_code", length = 20, nullable = false)
    private String clearingGlCode;

    /** IBC-OUT account number pattern for this branch. */
    @Column(name = "ibc_out_account_number", length = 50, nullable = false)
    private String ibcOutAccountNumber;

    /** IBC-IN account number pattern for this branch. */
    @Column(name = "ibc_in_account_number", length = 50, nullable = false)
    private String ibcInAccountNumber;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
