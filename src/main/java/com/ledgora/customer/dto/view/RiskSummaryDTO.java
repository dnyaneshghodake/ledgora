package com.ledgora.customer.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Risk & Governance summary for Customer 360° View — Risk tab. Aggregates hard limits, velocity
 * limits, fraud alerts, freeze levels, and violations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskSummaryDTO {

    /** Hard transaction limits for the tenant. */
    private List<HardLimitItem> hardLimits;

    /** Velocity limits for the tenant. */
    private List<VelocityLimitItem> velocityLimits;

    /** Fraud alerts linked to customer's accounts. */
    private List<FraudAlertItem> fraudAlerts;

    /** Accounts with non-NONE freeze levels. */
    private List<FrozenAccountItem> frozenAccounts;

    /** Hard ceiling violation audit entries. */
    private List<ViolationItem> hardCeilingViolations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HardLimitItem {
        private Long id;
        private String channel;
        private BigDecimal absoluteMaxAmount;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VelocityLimitItem {
        private Long id;
        private Long accountId;
        private String accountNumber;
        private Integer maxTxnCountPerHour;
        private BigDecimal maxTotalAmountPerHour;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FraudAlertItem {
        private Long id;
        private String accountNumber;
        private String alertType;
        private String status;
        private String details;
        private Integer observedCount;
        private BigDecimal observedAmount;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FrozenAccountItem {
        private Long accountId;
        private String accountNumber;
        private String accountName;
        private String freezeLevel;
        private String freezeReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ViolationItem {
        private Long id;
        private String action;
        private String details;
        private String username;
        private LocalDateTime timestamp;
    }
}
