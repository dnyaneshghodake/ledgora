package com.ledgora.transaction.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Suspense case detail for Transaction 360° View. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspenseDetailDTO {
    private Long id;
    private String reasonCode;
    private String reasonDetail;
    private String suspenseAccountNumber;
    private String suspenseAccountName;
    private String intendedAccountNumber;
    private String intendedAccountName;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDate businessDate;
    private String resolvedByUsername;
    private String resolutionCheckerUsername;
    private String resolutionRemarks;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Linked vouchers
    private String postedVoucherNumber;
    private String suspenseVoucherNumber;
    private String resolutionVoucherNumber;
}
