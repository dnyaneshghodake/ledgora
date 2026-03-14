package com.ledgora.transaction.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Inter-Branch Transfer detail for Transaction 360° View. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IbtDetailDTO {
    private Long id;
    private String status;
    private String fromBranchCode;
    private String fromBranchName;
    private String toBranchCode;
    private String toBranchName;
    private BigDecimal amount;
    private String currency;
    private LocalDate businessDate;
    private LocalDate settlementDate;
    private String narration;
    private String createdByUsername;
    private String approvedByUsername;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Branch-grouped vouchers
    private List<VoucherDetailDTO> branchAVouchers;
    private List<VoucherDetailDTO> branchBVouchers;
}
