package com.ledgora.approval.service;

import com.ledgora.approval.entity.ApprovalPolicy;
import com.ledgora.approval.repository.ApprovalPolicyRepository;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * CBS Approval Policy Decision Engine.
 *
 * <p>Determines whether a transaction should be auto-authorized or requires maker-checker approval.
 *
 * <p>Decision hierarchy: 1. Governance overrides (always require approval): reversals, backdated
 * entries 2. System-only transactions (always auto-authorize): interest accrual, charges, EOD via
 * BATCH channel 3. Policy table lookup: match by tenant + type + channel + amount range 4. Default
 * fallback: if no policy found, require approval (fail-safe)
 *
 * <p>The policy table is configurable per tenant, allowing each bank/branch to set their own teller
 * limits and approval thresholds.
 */
@Service
public class ApprovalPolicyService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalPolicyService.class);
    private final ApprovalPolicyRepository policyRepository;

    public ApprovalPolicyService(ApprovalPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    /**
     * Core decision: should this transaction be auto-authorized?
     *
     * @param tenantId the tenant context
     * @param transactionType the type of transaction (DEPOSIT, WITHDRAWAL, TRANSFER, etc.)
     * @param channel the originating channel (TELLER, ATM, ONLINE, MOBILE, BATCH)
     * @param amount the transaction amount
     * @param isReversal true if this is a reversal transaction
     * @param isBackdated true if posting date differs from current business date
     * @return true if auto-authorized, false if requires checker approval
     */
    public boolean isAutoAuthorized(
            Long tenantId,
            TransactionType transactionType,
            TransactionChannel channel,
            BigDecimal amount,
            boolean isReversal,
            boolean isBackdated) {

        // === GOVERNANCE OVERRIDES: Never auto-authorize ===
        if (isReversal) {
            log.info("Reversal transaction requires approval (governance rule)");
            return false;
        }
        if (isBackdated) {
            log.info("Backdated transaction requires approval (governance rule)");
            return false;
        }

        // === SYSTEM-ONLY: Always auto-authorize ===
        if (channel == TransactionChannel.BATCH) {
            log.debug("BATCH channel transaction auto-authorized (system-only)");
            return true;
        }

        // === POLICY TABLE LOOKUP ===
        String channelStr = channel != null ? channel.name() : "TELLER";
        String typeStr = transactionType.name();

        List<ApprovalPolicy> policies =
                policyRepository.findMatchingPolicies(tenantId, typeStr, channelStr);

        if (policies.isEmpty()) {
            // Default fallback: no policy configured = require approval (fail-safe)
            log.info(
                    "No approval policy found for tenant={} type={} channel={}. Defaulting to REQUIRE_APPROVAL.",
                    tenantId,
                    typeStr,
                    channelStr);
            return false;
        }

        // Evaluate policies in order (most specific first)
        for (ApprovalPolicy policy : policies) {
            if (amountInRange(amount, policy.getMinAmount(), policy.getMaxAmount())) {
                boolean autoAuth = Boolean.TRUE.equals(policy.getAutoAuthorizeFlag());
                log.info(
                        "Approval policy matched: id={} desc='{}' autoAuthorize={} for type={} channel={} amount={}",
                        policy.getId(),
                        policy.getDescription(),
                        autoAuth,
                        typeStr,
                        channelStr,
                        amount);
                return autoAuth;
            }
        }

        // Amount exceeds all configured ranges -> require approval
        log.info(
                "Amount {} exceeds all policy ranges for type={} channel={}. Requiring approval.",
                amount,
                typeStr,
                channelStr);
        return false;
    }

    /** Simplified overload for common use cases (no reversal, not backdated). */
    public boolean isAutoAuthorized(
            Long tenantId,
            TransactionType transactionType,
            TransactionChannel channel,
            BigDecimal amount) {
        return isAutoAuthorized(tenantId, transactionType, channel, amount, false, false);
    }

    /** Get all active policies for a tenant (for admin UI display). */
    public List<ApprovalPolicy> getActivePolicies(Long tenantId) {
        return policyRepository.findByTenant_IdAndIsActiveTrue(tenantId);
    }

    /** Get all policies for a tenant (including inactive, for admin management). */
    public List<ApprovalPolicy> getAllPolicies(Long tenantId) {
        return policyRepository.findByTenant_Id(tenantId);
    }

    private boolean amountInRange(BigDecimal amount, BigDecimal min, BigDecimal max) {
        if (amount == null) {
            return false;
        }
        if (min != null && amount.compareTo(min) < 0) {
            return false;
        }
        if (max != null && amount.compareTo(max) > 0) {
            return false;
        }
        return true;
    }
}
