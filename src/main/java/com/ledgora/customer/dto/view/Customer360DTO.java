package com.ledgora.customer.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level DTO for Customer 360° View.
 *
 * <p>Assembled by Customer360Service from existing repositories. No entity leakage to JSP — all
 * data is pre-mapped into this DTO tree.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer360DTO {

    // ── CIF Snapshot (from Customer entity) ──

    private Long customerId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String customerType;
    private String riskCategory;
    private String approvalStatus;
    private String kycStatus;
    private String freezeLevel;
    private String freezeReason;
    private String nationalId;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── KPI Cards (derived from existing services/repositories) ──

    private int totalAccounts;
    private BigDecimal totalLedgerBalance;
    private BigDecimal totalAvailableBalance;
    private BigDecimal totalLienAmount;
    private BigDecimal openSuspenseAmount;
    private long openSuspenseCount;
    private BigDecimal openIbtAmount;
    private long openIbtCount;
    private long openFraudAlertCount;
    private long accountsUnderReviewCount;

    // ── Tab Data ──

    private List<AccountSummaryDTO> accounts;
    private List<TransactionSummaryDTO> transactions;
    private long totalTransactionCount;
    private List<IbtSummaryDTO> ibtTransfers;
    private long unsettledIbtCount;
    private BigDecimal netClearingExposure;
    private List<SuspenseSummaryDTO> suspenseCases;
    private BigDecimal suspenseGlBalance;
    private RiskSummaryDTO riskSummary;
    private List<AuditTimelineDTO> auditTrail;
}
