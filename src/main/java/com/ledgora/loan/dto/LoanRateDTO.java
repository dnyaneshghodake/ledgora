package com.ledgora.loan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Loan Rate DTO — used for rate creation and benchmark reset requests.
 *
 * <p>Per RBI Fair Practices Code:
 *
 * <ul>
 *   <li>For FIXED products: only effectiveRate and effectiveDate are required
 *   <li>For FLOATING products: benchmarkName, benchmarkRate, spread are required;
 *       effectiveRate = benchmarkRate + spread (computed by service)
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRateDTO {

    private Long productId;

    /** Benchmark name — e.g. "REPO", "MCLR_1Y", "EBLR". Required for FLOATING. */
    private String benchmarkName;

    /** Benchmark rate value. Required for FLOATING. */
    private BigDecimal benchmarkRate;

    /** Spread over benchmark. Required for FLOATING, optional for FIXED. */
    private BigDecimal spread;

    /** Effective annual rate. For FIXED: provided directly. For FLOATING: computed. */
    private BigDecimal effectiveRate;

    /** Date from which the new rate applies. */
    private LocalDate effectiveDate;

    /** Reason for rate change. */
    private String changeReason;

    /** Optional remarks. */
    private String remarks;
}
