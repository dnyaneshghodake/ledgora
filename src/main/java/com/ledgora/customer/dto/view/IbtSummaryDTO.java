package com.ledgora.customer.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IBT summary for Customer 360° View — IBT Exposure tab. Populated from InterBranchTransfer entity
 * with tenant isolation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IbtSummaryDTO {

    private Long ibtId;
    private String fromBranchName;
    private String fromBranchCode;
    private String toBranchName;
    private String toBranchCode;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDate businessDate;
    private LocalDateTime createdAt;
}
