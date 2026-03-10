package com.ledgora.approval.service;

import com.ledgora.approval.entity.HardTransactionLimit;
import com.ledgora.approval.repository.HardTransactionLimitRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.exception.GovernanceException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Hard Transaction Ceiling Service — absolute, non-bypassable transaction limits.
 *
 * <p>RBI Risk Appetite Framework / Internal Control — Absolute Exposure Limits:
 *
 * <ul>
 *   <li>Enforced BEFORE voucher creation — no transaction exceeding the ceiling can enter the
 *       system
 *   <li>No role (including ADMIN) may bypass this limit
 *   <li>No configuration override path exists at runtime
 *   <li>All violations are logged to governance audit trail and increment Micrometer metric
 *   <li>Limits are per tenant + channel (with default fallback)
 * </ul>
 *
 * <p>This is a safety net above the approval policy engine. Even if ApprovalPolicyService allows
 * auto-authorization, the hard ceiling blocks the transaction if the amount exceeds the absolute
 * maximum.
 */
@Service
public class HardTransactionCeilingService {

    private static final Logger log = LoggerFactory.getLogger(HardTransactionCeilingService.class);

    private final HardTransactionLimitRepository hardLimitRepository;
    private final AuditService auditService;
    private final Counter hardLimitBlockedCounter;

    public HardTransactionCeilingService(
            HardTransactionLimitRepository hardLimitRepository,
            AuditService auditService,
            MeterRegistry meterRegistry) {
        this.hardLimitRepository = hardLimitRepository;
        this.auditService = auditService;
        this.hardLimitBlockedCounter =
                Counter.builder("ledgora.hard_limit.blocked")
                        .description(
                                "Total transactions blocked by hard transaction ceiling enforcement")
                        .register(meterRegistry);
    }

    /**
     * Validate that the transaction amount does not exceed the absolute hard ceiling.
     *
     * <p>Resolution order: 1. Channel-specific limit from hard_transaction_limits 2. Default
     * (channel=null) limit from hard_transaction_limits 3. If no limit is configured, the
     * transaction is allowed (fail-open for unconfigured tenants — configure limits for production)
     *
     * <p>GOVERNANCE: This method must be called BEFORE any voucher creation or transaction
     * persistence. It is the first line of defense.
     *
     * @param tenantId the tenant ID
     * @param channel the transaction channel (may be null)
     * @param amount the transaction amount to validate
     * @param userId the user initiating the transaction (for audit logging)
     * @throws GovernanceException if the amount exceeds the hard ceiling — NO OVERRIDE PATH
     */
    public void enforceHardCeiling(
            Long tenantId, TransactionChannel channel, BigDecimal amount, Long userId) {

        // 1. Try channel-specific limit
        HardTransactionLimit limit = null;
        if (channel != null) {
            limit =
                    hardLimitRepository
                            .findByTenantIdAndChannelAndIsActiveTrue(tenantId, channel)
                            .orElse(null);
        }

        // 2. Fall back to default (channel=null) limit
        if (limit == null) {
            limit =
                    hardLimitRepository
                            .findByTenantIdAndChannelIsNullAndIsActiveTrue(tenantId)
                            .orElse(null);
        }

        // 3. No limit configured — allow (fail-open)
        if (limit == null) {
            return;
        }

        // ── HARD CEILING ENFORCEMENT — NO BYPASS ──
        if (amount.compareTo(limit.getAbsoluteMaxAmount()) > 0) {
            // Increment metric
            hardLimitBlockedCounter.increment();

            // Log to governance audit trail
            String channelStr = channel != null ? channel.name() : "DEFAULT";
            String detail =
                    "HARD_LIMIT_EXCEEDED: amount="
                            + amount
                            + " exceeds absolute ceiling="
                            + limit.getAbsoluteMaxAmount()
                            + " for tenant="
                            + tenantId
                            + " channel="
                            + channelStr
                            + " userId="
                            + userId;

            auditService.logEvent(
                    userId, "HARD_LIMIT_EXCEEDED", "GOVERNANCE", null, detail, null);

            log.warn(
                    "HARD CEILING BLOCKED: tenant={} channel={} amount={} ceiling={} userId={}",
                    tenantId,
                    channelStr,
                    amount,
                    limit.getAbsoluteMaxAmount(),
                    userId);

            // Throw GovernanceException — NO OVERRIDE, NO FALLBACK
            throw new GovernanceException(
                    "HARD_LIMIT_EXCEEDED",
                    "Transaction amount "
                            + amount
                            + " exceeds the absolute hard ceiling of "
                            + limit.getAbsoluteMaxAmount()
                            + " for channel "
                            + channelStr
                            + ". This limit cannot be overridden by any role or configuration. "
                            + "Contact Risk Management to adjust hard_transaction_limits.");
        }
    }
}
