package com.ledgora.customer.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Suspense case summary for Customer 360° View — Suspense Exposure tab. Populated from SuspenseCase
 * entity with tenant isolation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspenseSummaryDTO {

    private Long caseId;
    private String accountNumber;
    private String accountName;
    private String reasonCode;
    private String reasonDetail;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
