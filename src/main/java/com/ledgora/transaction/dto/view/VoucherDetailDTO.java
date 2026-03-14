package com.ledgora.transaction.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Voucher detail for Transaction 360° View with expandable ledger entry linkage. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherDetailDTO {
    private Long id;
    private String voucherNumber;
    private String branchCode;
    private String branchName;
    private String drCr;
    private String accountNumber;
    private String accountName;
    private String glCode;
    private BigDecimal amount;
    private String authFlag;
    private String postFlag;
    private String cancelFlag;
    private Long batchId;
    private LocalDate postingDate;
    private Long scrollNo;
    private String status;

    // Expandable detail: linked ledger entry
    private Long ledgerEntryId;
    private String ledgerEntryType;
    private BigDecimal ledgerAmount;
    private BigDecimal balanceAfter;

    // Reversal reference
    private Long reversalOfVoucherId;
    private String reversalOfVoucherNumber;

    // Maker-Checker
    private String makerUsername;
    private String checkerUsername;
}
