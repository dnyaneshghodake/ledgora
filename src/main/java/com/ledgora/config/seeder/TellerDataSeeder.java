package com.ledgora.config.seeder;

import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.TellerStatus;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.teller.entity.CashDenominationMaster;
import com.ledgora.teller.entity.TellerMaster;
import com.ledgora.teller.entity.VaultMaster;
import com.ledgora.teller.repository.CashDenominationMasterRepository;
import com.ledgora.teller.repository.TellerMasterRepository;
import com.ledgora.teller.repository.VaultMasterRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 11 — Teller Operations master data. Seeds:
 *
 * <ul>
 *   <li>CashDenominationMaster (INR denominations: 2000..10)
 *   <li>VaultMaster (one per branch, dual-custody, ₹2Cr limit)
 *   <li>TellerMaster (one per ROLE_TELLER user, ASSIGNED status, RBI-compliant limits)
 * </ul>
 *
 * <p>Does NOT create TellerSession — session opening is a controlled operational event requiring
 * maker-checker authorization and denomination capture.
 *
 * <p>Idempotent: checks count before inserting.
 */
@Component
public class TellerDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TellerDataSeeder.class);

    private final CashDenominationMasterRepository denomRepository;
    private final VaultMasterRepository vaultRepository;
    private final TellerMasterRepository tellerRepository;
    private final UserRepository userRepository;

    public TellerDataSeeder(
            CashDenominationMasterRepository denomRepository,
            VaultMasterRepository vaultRepository,
            TellerMasterRepository tellerRepository,
            UserRepository userRepository) {
        this.denomRepository = denomRepository;
        this.vaultRepository = vaultRepository;
        this.tellerRepository = tellerRepository;
        this.userRepository = userRepository;
    }

    public void seed(Tenant tenant, Branch hq, Branch br1, Branch br2) {
        seedDenominations();
        seedVaults(tenant, hq, br1, br2);
        seedTellerMasters(tenant, hq, br1, br2);
    }

    // ── CashDenominationMaster ──────────────────────────────────────────────

    private void seedDenominations() {
        if (denomRepository.count() > 0) {
            log.info("  [Denominations] Already seeded — skipping");
            return;
        }
        int[][] denoms = {
            {2000, 1},
            {500, 2},
            {200, 3},
            {100, 4},
            {50, 5},
            {20, 6},
            {10, 7}
        };
        for (int[] d : denoms) {
            denomRepository.save(
                    CashDenominationMaster.builder()
                            .denominationValue(new BigDecimal(d[0] + ".0000"))
                            .label("\u20B9" + d[0])
                            .active(true)
                            .sortOrder(d[1])
                            .build());
        }
        log.info("  [Denominations] 7 INR denominations seeded (₹2000..₹10)");
    }

    // ── VaultMaster ─────────────────────────────────────────────────────────

    private void seedVaults(Tenant tenant, Branch hq, Branch br1, Branch br2) {
        for (Branch branch : new Branch[] {hq, br1, br2}) {
            if (vaultRepository.findByBranchId(branch.getId()).isPresent()) {
                continue;
            }
            vaultRepository.save(
                    VaultMaster.builder()
                            .tenant(tenant)
                            .branch(branch)
                            .currentBalance(BigDecimal.ZERO)
                            .holdingLimit(new BigDecimal("20000000.0000"))
                            .dualCustodyFlag(true)
                            .build());
        }
        log.info("  [Vaults] VaultMaster seeded for HQ001, BR001, BR002 (₹2Cr limit, dual-custody)");
    }

    // ── TellerMaster ────────────────────────────────────────────────────────

    private void seedTellerMasters(Tenant tenant, Branch hq, Branch br1, Branch br2) {
        // teller1 → BR001, teller2 → BR002
        seedOneTeller(tenant, "teller1", br1);
        seedOneTeller(tenant, "teller2", br2);
        // NOTE: teller3 belongs to secondTenant (seeded in UserDataSeeder) — do NOT seed under
        // defaultTenant here. That would create a cross-tenant TellerMaster (TellerMaster.tenant !=
        // User.tenant), making the record inaccessible via CashEngineService.requireTellerMaster().
        log.info(
                "  [Tellers] TellerMaster seeded (status=ASSIGNED, limits: deposit=₹2L, withdrawal=₹50K, daily=₹5L, holding=₹10L)");
    }

    private void seedOneTeller(Tenant tenant, String username, Branch branch) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }
        if (tellerRepository.findByBranchIdAndUserId(branch.getId(), user.getId()).isPresent()) {
            return;
        }
        tellerRepository.save(
                TellerMaster.builder()
                        .tenant(tenant)
                        .branch(branch)
                        .user(user)
                        .status(TellerStatus.ASSIGNED)
                        .singleTxnLimitDeposit(new BigDecimal("200000.0000"))
                        .singleTxnLimitWithdrawal(new BigDecimal("50000.0000"))
                        .dailyTxnLimit(new BigDecimal("500000.0000"))
                        .cashHoldingLimit(new BigDecimal("1000000.0000"))
                        .activeFlag(true)
                        .build());
    }
}
