package com.ledgora.approval.service;

import com.ledgora.approval.entity.ApprovalPolicy;
import com.ledgora.approval.repository.ApprovalPolicyRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds default CBS approval policies on startup if none exist.
 *
 * Default policy matrix (per CBS guidelines):
 *
 * | Type       | Channel        | Max Amount  | Auto? | Description                                  |
 * |------------|----------------|-------------|-------|----------------------------------------------|
 * | DEPOSIT    | TELLER         | 200,000     | YES   | Low-value teller deposit                     |
 * | DEPOSIT    | TELLER         | null        | NO    | High-value teller deposit needs approval     |
 * | DEPOSIT    | ATM            | null        | YES   | ATM deposit auto-authorized                  |
 * | DEPOSIT    | ONLINE         | null        | YES   | Online deposit auto-authorized               |
 * | DEPOSIT    | MOBILE         | null        | YES   | Mobile deposit auto-authorized               |
 * | WITHDRAWAL | TELLER         | 200,000     | YES   | Low-value teller withdrawal                  |
 * | WITHDRAWAL | TELLER         | null        | NO    | High-value teller withdrawal needs approval  |
 * | WITHDRAWAL | ATM            | null        | YES   | ATM withdrawal auto-authorized               |
 * | WITHDRAWAL | ONLINE         | null        | YES   | Online withdrawal auto-authorized            |
 * | WITHDRAWAL | MOBILE         | null        | YES   | Mobile withdrawal auto-authorized            |
 * | TRANSFER   | TELLER         | 500,000     | YES   | Low-value teller transfer                    |
 * | TRANSFER   | TELLER         | null        | NO    | High-value teller transfer needs approval    |
 * | TRANSFER   | ATM            | null        | YES   | ATM transfer auto-authorized                 |
 * | TRANSFER   | ONLINE         | null        | YES   | Online transfer auto-authorized              |
 * | TRANSFER   | MOBILE         | null        | YES   | Mobile transfer auto-authorized              |
 * | DEPOSIT    | BATCH          | null        | YES   | System/batch deposit always auto             |
 * | WITHDRAWAL | BATCH          | null        | YES   | System/batch withdrawal always auto          |
 * | TRANSFER   | BATCH          | null        | YES   | System/batch transfer always auto            |
 *
 * Reversals and backdated entries are blocked by governance overrides in ApprovalPolicyService
 * (hardcoded, not configurable — always require approval).
 */
@Component
public class ApprovalPolicyInitializer {

    private static final Logger log = LoggerFactory.getLogger(ApprovalPolicyInitializer.class);
    private final ApprovalPolicyRepository policyRepository;
    private final TenantRepository tenantRepository;

    public ApprovalPolicyInitializer(ApprovalPolicyRepository policyRepository,
                                      TenantRepository tenantRepository) {
        this.policyRepository = policyRepository;
        this.tenantRepository = tenantRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultPolicies() {
        if (policyRepository.count() > 0) {
            log.debug("Approval policies already exist, skipping seed.");
            return;
        }

        List<Tenant> tenants = tenantRepository.findByStatus("ACTIVE");
        if (tenants.isEmpty()) {
            log.info("No active tenants found, skipping approval policy seed.");
            return;
        }

        for (Tenant tenant : tenants) {
            seedForTenant(tenant);
        }
        log.info("Default approval policies seeded for {} tenant(s).", tenants.size());
    }

    private void seedForTenant(Tenant tenant) {
        BigDecimal tellerLimit = new BigDecimal("200000.0000");
        BigDecimal transferLimit = new BigDecimal("500000.0000");

        // --- DEPOSIT ---
        createPolicy(tenant, "DEPOSIT", "TELLER", null, tellerLimit, true,
                "Low-value teller deposit (auto-authorized up to " + tellerLimit + ")");
        createPolicy(tenant, "DEPOSIT", "TELLER", tellerLimit.add(BigDecimal.ONE), null, false,
                "High-value teller deposit (requires checker approval)");
        createPolicy(tenant, "DEPOSIT", "ATM", null, null, true,
                "ATM deposit (auto-authorized, channel-limited)");
        createPolicy(tenant, "DEPOSIT", "ONLINE", null, null, true,
                "Online deposit (auto-authorized)");
        createPolicy(tenant, "DEPOSIT", "MOBILE", null, null, true,
                "Mobile deposit (auto-authorized)");

        // --- WITHDRAWAL ---
        createPolicy(tenant, "WITHDRAWAL", "TELLER", null, tellerLimit, true,
                "Low-value teller withdrawal (auto-authorized up to " + tellerLimit + ")");
        createPolicy(tenant, "WITHDRAWAL", "TELLER", tellerLimit.add(BigDecimal.ONE), null, false,
                "High-value teller withdrawal (requires checker approval)");
        createPolicy(tenant, "WITHDRAWAL", "ATM", null, null, true,
                "ATM withdrawal (auto-authorized, channel-limited)");
        createPolicy(tenant, "WITHDRAWAL", "ONLINE", null, null, true,
                "Online withdrawal (auto-authorized)");
        createPolicy(tenant, "WITHDRAWAL", "MOBILE", null, null, true,
                "Mobile withdrawal (auto-authorized)");

        // --- TRANSFER ---
        createPolicy(tenant, "TRANSFER", "TELLER", null, transferLimit, true,
                "Low-value teller transfer (auto-authorized up to " + transferLimit + ")");
        createPolicy(tenant, "TRANSFER", "TELLER", transferLimit.add(BigDecimal.ONE), null, false,
                "High-value teller transfer (requires checker approval)");
        createPolicy(tenant, "TRANSFER", "ATM", null, null, true,
                "ATM transfer (auto-authorized)");
        createPolicy(tenant, "TRANSFER", "ONLINE", null, null, true,
                "Online transfer (auto-authorized)");
        createPolicy(tenant, "TRANSFER", "MOBILE", null, null, true,
                "Mobile transfer (auto-authorized)");

        // --- BATCH/SYSTEM (always auto) ---
        createPolicy(tenant, "DEPOSIT", "BATCH", null, null, true,
                "System/batch deposit (always auto-authorized)");
        createPolicy(tenant, "WITHDRAWAL", "BATCH", null, null, true,
                "System/batch withdrawal (always auto-authorized)");
        createPolicy(tenant, "TRANSFER", "BATCH", null, null, true,
                "System/batch transfer (always auto-authorized)");
    }

    private void createPolicy(Tenant tenant, String txnType, String channel,
                               BigDecimal minAmount, BigDecimal maxAmount,
                               boolean autoAuthorize, String description) {
        ApprovalPolicy policy = ApprovalPolicy.builder()
                .tenant(tenant)
                .transactionType(txnType)
                .channel(channel)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .autoAuthorizeFlag(autoAuthorize)
                .approvalRequiredFlag(!autoAuthorize)
                .roleAllowed(autoAuthorize ? null : "ROLE_CHECKER,ROLE_MANAGER,ROLE_BRANCH_MANAGER")
                .description(description)
                .isActive(true)
                .build();
        policyRepository.save(policy);
    }
}
