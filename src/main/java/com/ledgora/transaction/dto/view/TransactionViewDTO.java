package com.ledgora.transaction.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CBS-Grade Transaction 360° View DTO. Aggregates all transaction-related data for the unified
 * detail screen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionViewDTO {

    // Section 1: Transaction Header
    private Long transactionId;
    private String transactionRef;
    private String transactionType;
    private String channel;
    private LocalDate businessDate;
    private LocalDateTime valueDate;
    private String status;
    private String makerUsername;
    private String checkerUsername;
    private LocalDateTime makerTimestamp;
    private LocalDateTime checkerTimestamp;
    private String checkerRemarks;
    private String branchCode;
    private String tenantCode;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String narration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Reversal reference
    private Long reversalOfTransactionId;
    private String reversalOfTransactionRef;

    // Section 2: Account Impact Summary
    private List<AccountImpactDTO> accountImpacts;

    // Section 3: Voucher Details
    private List<VoucherDetailDTO> vouchers;

    // Section 4: Ledger Entries
    private List<LedgerEntryDTO> ledgerEntries;

    // Section 5: IBT Panel
    private boolean ibtTransaction;
    private IbtDetailDTO ibtDetail;

    // Section 6: Suspense Panel
    private boolean suspenseTransaction;
    private List<SuspenseDetailDTO> suspenseCases;

    // Section 7: Audit Trail
    private List<AuditTimelineDTO> auditTrail;

    // Authorization context
    private boolean pendingApproval;
    private boolean checkerView;
}
