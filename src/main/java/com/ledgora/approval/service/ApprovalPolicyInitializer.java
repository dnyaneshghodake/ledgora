package com.ledgora.approval.service;

import com.ledgora.approval.entity.ApprovalPolicy;
import com.ledgora.approval.repository.ApprovalPolicyRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds default CBS approval policies on startup if none exist.
 *
 * <p>Default policy matrix (per CBS guidelines):
 *
 * <p>| Type | Channel | Max Amount | Auto? | Description |
 * |------------|----------------|-------------|-------|----------------------------------------------|
 * | DEPOSIT | TELLER | 200,000 | YES | Low-value teller deposit (low risk, cash in) | | DEPOSIT |
 * TELLER | null | NO | High-value teller deposit needs approval | | DEPOSIT | ATM | null | YES |
 * ATM deposit auto-authorized | | DEPOSIT | ONLINE | null | YES | Online deposit auto-authorized |
 * | DEPOSIT | MOBILE | null | YES | Mobile deposit auto-authorized | | WITHDRAWAL | TELLER |
 * 100,000 | YES | Low-value teller withdrawal (higher risk) | | WITHDRAWAL | TELLER | null | NO |
 * High-value teller withdrawal needs approval | | WITHDRAWAL | ATM | null | YES | ATM withdrawal
 * auto-authorized | | WITHDRAWAL | ONLINE | null | YES | Online withdrawal auto-authorized | |
 * WITHDRAWAL | MOBILE | null | YES | Mobile withdrawal auto-authorized | | TRANSFER | TELLER |
 * 500,000 | YES | Low-value teller transfer | | TRANSFER | TELLER | null | NO | High-value teller
 * transfer needs approval | | TRANSFER | ATM | null | YES | ATM transfer auto-authorized | |
 * TRANSFER | ONLINE | null | YES | Online transfer auto-authorized | | TRANSFER | MOBILE | null |
 * YES | Mobile transfer auto-authorized | | DEPOSIT | BATCH | null | YES | System/batch deposit
 * always auto | | WITHDRAWAL | BATCH | null | YES | System/batch withdrawal always auto | |
 * TRANSFER | BATCH | null | YES | System/batch transfer always auto |
 *
 * <p>Reversals and backdated entries are blocked by governance overrides in ApprovalPolicyService
 * (hardcoded, not configurable — always require approval).
 */
@Component
public class ApprovalPolicyInitializer {

    private static final Logger log = LoggerFactory.getLogger(ApprovalPolicyInitializer.class);
    private final ApprovalPolicyRepository policyRepository;
    private final TenantRepository tenantRepository;

    public ApprovalPolicyInitializer(
            ApprovalPolicyRepository policyRepository, TenantRepository tenantRepository) {
        this.policyRepository = policyRepository;
        this.tenantRepository = tenantRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultPolicies() {
        List<Tenant> tenants = tenantRepository.findByStatus("ACTIVE");
        if (tenants.isEmpty()) {
            log.info("No active tenants found, skipping approval policy seed.");
            return;
        }

        int seededCount = 0;
        for (Tenant tenant : tenants) {
            // Per-tenant check: only seed if this specific tenant has no policies
            List<com.ledgora.approval.entity.ApprovalPolicy> existingPolicies =
                    policyRepository.findByTenant_Id(tenant.getId());
            if (existingPolicies.isEmpty()) {
                seedForTenant(tenant);
                seededCount++;
            }
        }
        if (seededCount > 0) {
            log.info("Default approval policies seeded for {} tenant(s).", seededCount);
        } else {
            log.debug("All active tenants already have approval policies, skipping seed.");
        }
    }

    private void seedForTenant(Tenant tenant) {
        BigDecimal depositLimit =
                new BigDecimal("200000.0000"); // ₹2L - low risk (cash in), AML-monitored
        BigDecimal withdrawalLimit = new BigDecimal("100000.0000"); // ₹1L - higher risk (cash out)
        BigDecimal transferLimit = new BigDecimal("500000.0000"); // ₹5L - internal movement

        // --- DEPOSIT (low risk: cash coming into bank, AML monitoring) ---
        createPolicy(
                tenant,
                "DEPOSIT",
                "TELLER",
                null,
                depositLimit,
                true,
                "Low-value teller deposit (auto-authorized up to " + depositLimit + ")");
        createPolicy(
                tenant,
                "DEPOSIT",
                "TELLER",
                depositLimit.add(new BigDecimal("0.0001")),
                null,
                false,
                "High-value teller deposit (requires checker approval)");
        createPolicy(
                tenant,
                "DEPOSIT",
                "ATM",
                null,
                null,
                true,
                "ATM deposit (auto-authorized, channel-limited)");
        createPolicy(
                tenant, "DEPOSIT", "ONLINE", null, null, true, "Online deposit (auto-authorized)");
        createPolicy(
                tenant, "DEPOSIT", "MOBILE", null, null, true, "Mobile deposit (auto-authorized)");

        // --- WITHDRAWAL (higher risk: cash leaving bank) ---
        createPolicy(
                tenant,
                "WITHDRAWAL",
                "TELLER",
                null,
                withdrawalLimit,
                true,
                "Low-value teller withdrawal (auto-authorized up to " + withdrawalLimit + ")");
        createPolicy(
                tenant,
                "WITHDRAWAL",
                "TELLER",
                withdrawalLimit.add(new BigDecimal("0.0001")),
                null,
                false,
                "High-value teller withdrawal (requires checker approval)");
        createPolicy(
                tenant,
                "WITHDRAWAL",
                "ATM",
                null,
                null,
                true,
                "ATM withdrawal (auto-authorized, channel-limited)");
        createPolicy(
                tenant,
                "WITHDRAWAL",
                "ONLINE",
                null,
                null,
                true,
                "Online withdrawal (auto-authorized)");
        createPolicy(
                tenant,
                "WITHDRAWAL",
                "MOBILE",
                null,
                null,
                true,
                "Mobile withdrawal (auto-authorized)");

        // --- TRANSFER ---
        createPolicy(
                tenant,
                "TRANSFER",
                "TELLER",
                null,
                transferLimit,
                true,
                "Low-value teller transfer (auto-authorized up to " + transferLimit + ")");
        createPolicy(
                tenant,
                "TRANSFER",
                "TELLER",
                transferLimit.add(new BigDecimal("0.0001")),
                null,
                false,
                "High-value teller transfer (requires checker approval)");
        createPolicy(tenant, "TRANSFER", "ATM", null, null, true, "ATM transfer (auto-authorized)");
        createPolicy(
                tenant,
                "TRANSFER",
                "ONLINE",
                null,
                null,
                true,
                "Online transfer (auto-authorized)");
        createPolicy(
                tenant,
                "TRANSFER",
                "MOBILE",
                null,
                null,
                true,
                "Mobile transfer (auto-authorized)");

        // --- BATCH/SYSTEM (always auto) ---
        createPolicy(
                tenant,
                "DEPOSIT",
                "BATCH",
                null,
                null,
                true,
                "System/batch deposit (always auto-authorized)");
        createPolicy(
                tenant,
                "WITHDRAWAL",
                "BATCH",
                null,
                null,
                true,
                "System/batch withdrawal (always auto-authorized)");
        createPolicy(
                tenant,
                "TRANSFER",
                "BATCH",
                null,
                null,
                true,
                "System/batch transfer (always auto-authorized)");
    }

    private void createPolicy(
            Tenant tenant,
            String txnType,
            String channel,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            boolean autoAuthorize,
            String description) {
        ApprovalPolicy policy =
                ApprovalPolicy.builder()
                        .tenant(tenant)
                        .transactionType(txnType)
                        .channel(channel)
                        .minAmount(minAmount)
                        .maxAmount(maxAmount)
                        .autoAuthorizeFlag(autoAuthorize)
                        .approvalRequiredFlag(!autoAuthorize)
                        .roleAllowed(
                                autoAuthorize
                                        ? null
                                        : "ROLE_CHECKER,ROLE_MANAGER,ROLE_BRANCH_MANAGER")
                        .description(description)
                        .isActive(true)
                        .build();
        policyRepository.save(policy);
    }
}
