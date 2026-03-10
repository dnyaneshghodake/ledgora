package com.ledgora.stress.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Bulk data generator for EOD stress testing.
 *
 * <p>Active only in the "stress" profile. Uses existing TransactionService methods — no direct
 * repository inserts for financial data. All business invariants (double-entry, batch totals,
 * idempotency) are preserved because we go through the normal service layer.
 *
 * <p>Generates accounts, deposits, and cross-branch transfers to create realistic EOD load.
 */
@Service
@Profile("stress")
public class EodLoadGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(EodLoadGeneratorService.class);

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final BranchRepository branchRepository;
    private final TenantRepository tenantRepository;
    private final GeneralLedgerRepository glRepository;
    private final Random random = new Random(42); // deterministic seed for reproducibility

    public EodLoadGeneratorService(
            TransactionService transactionService,
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository,
            BranchRepository branchRepository,
            TenantRepository tenantRepository,
            GeneralLedgerRepository glRepository) {
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.branchRepository = branchRepository;
        this.tenantRepository = tenantRepository;
        this.glRepository = glRepository;
    }

    /**
     * Generate stress test load.
     *
     * @param tenantId target tenant
     * @param accountCount accounts to create per branch
     * @param transactionCount total transactions to generate
     * @param ibtRatioPercent percentage of transfers that are cross-branch
     * @return number of IBT transfers actually generated
     */
    public int generateLoad(
            Long tenantId,
            int accountCount,
            int transactionCount,
            int ibtRatioPercent) {

        // Setup security context for TransactionService
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "stress-runner",
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));
        TenantContextHolder.setTenantId(tenantId);

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () -> new RuntimeException("Tenant not found: " + tenantId));

        // Get or create branches
        List<Branch> branches = branchRepository.findAll();
        if (branches.size() < 2) {
            throw new RuntimeException(
                    "Stress test requires at least 2 branches. Found: " + branches.size());
        }

        // Resolve GL for accounts
        GeneralLedger gl =
                glRepository
                        .findByGlCode("2110")
                        .orElseGet(
                                () ->
                                        glRepository
                                                .findByGlCode("1100")
                                                .orElseThrow(
                                                        () ->
                                                                new RuntimeException(
                                                                        "No GL account found for stress test")));

        // Step 1: Generate accounts across branches
        List<Account> generatedAccounts = new ArrayList<>();
        int branchIndex = 0;
        for (int i = 0; i < accountCount; i++) {
            Branch branch = branches.get(branchIndex % branches.size());
            branchIndex++;

            String accNo = "STRESS-" + tenantId + "-" + String.format("%06d", i);
            Account account =
                    accountRepository
                            .findByAccountNumberAndTenantId(accNo, tenantId)
                            .orElseGet(
                                    () -> {
                                        Account a =
                                                Account.builder()
                                                        .tenant(tenant)
                                                        .accountNumber(accNo)
                                                        .accountName("Stress Account " + accNo)
                                                        .accountType(AccountType.SAVINGS)
                                                        .status(AccountStatus.ACTIVE)
                                                        .balance(BigDecimal.ZERO)
                                                        .currency("INR")
                                                        .branch(branch)
                                                        .homeBranch(branch)
                                                        .glAccountCode(gl.getGlCode())
                                                        .build();
                                        return accountRepository.save(a);
                                    });
            // Seed balance
            seedBalance(account, new BigDecimal("100000.00"));
            generatedAccounts.add(account);
        }
        log.info("Stress: {} accounts created/seeded across {} branches", accountCount, branches.size());

        // Step 2: Generate transactions
        int ibtCount = 0;
        int depositsCreated = 0;
        int transfersCreated = 0;

        for (int i = 0; i < transactionCount; i++) {
            try {
                boolean doIbt =
                        random.nextInt(100) < ibtRatioPercent && generatedAccounts.size() >= 2;

                if (doIbt) {
                    // Cross-branch transfer: pick accounts from different branches
                    Account src = pickAccountFromBranch(generatedAccounts, branches.get(0).getId());
                    Account dst = pickAccountFromBranch(generatedAccounts, branches.get(1).getId());
                    if (src != null && dst != null) {
                        BigDecimal amount =
                                new BigDecimal(random.nextInt(900) + 100)
                                        .setScale(2, java.math.RoundingMode.HALF_UP);
                        transactionService.transfer(
                                TransactionDTO.builder()
                                        .transactionType("TRANSFER")
                                        .sourceAccountNumber(src.getAccountNumber())
                                        .destinationAccountNumber(dst.getAccountNumber())
                                        .amount(amount)
                                        .currency("INR")
                                        .channel(TransactionChannel.BATCH.name())
                                        .clientReferenceId("STRESS-IBT-" + tenantId + "-" + i)
                                        .description("Stress IBT #" + i)
                                        .narration("Stress inter-branch transfer")
                                        .build());
                        ibtCount++;
                        transfersCreated++;
                    }
                } else {
                    // Simple deposit
                    Account target =
                            generatedAccounts.get(random.nextInt(generatedAccounts.size()));
                    BigDecimal amount =
                            new BigDecimal(random.nextInt(9000) + 1000)
                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                    transactionService.deposit(
                            TransactionDTO.builder()
                                    .transactionType("DEPOSIT")
                                    .destinationAccountNumber(target.getAccountNumber())
                                    .amount(amount)
                                    .currency("INR")
                                    .channel(TransactionChannel.BATCH.name())
                                    .clientReferenceId("STRESS-DEP-" + tenantId + "-" + i)
                                    .description("Stress deposit #" + i)
                                    .narration("Stress deposit")
                                    .build());
                    depositsCreated++;
                }

                if ((i + 1) % 500 == 0) {
                    log.info(
                            "Stress: {}/{} transactions generated (deposits={}, IBTs={})",
                            i + 1,
                            transactionCount,
                            depositsCreated,
                            ibtCount);
                }
            } catch (Exception e) {
                log.warn("Stress: transaction {} failed: {}", i, e.getMessage());
            }
        }

        log.info(
                "Stress load generation complete: {} deposits, {} IBT transfers, {} total",
                depositsCreated,
                ibtCount,
                depositsCreated + transfersCreated);

        return ibtCount;
    }

    private Account pickAccountFromBranch(List<Account> accounts, Long branchId) {
        return accounts.stream()
                .filter(a -> a.getBranch() != null && a.getBranch().getId().equals(branchId))
                .findFirst()
                .orElse(null);
    }

    private void seedBalance(Account account, BigDecimal amount) {
        AccountBalance balance =
                accountBalanceRepository
                        .findByAccountId(account.getId())
                        .orElse(AccountBalance.builder().account(account).build());
        balance.setActualTotalBalance(amount);
        balance.setActualClearedBalance(amount);
        balance.setAvailableBalance(amount);
        balance.setLedgerBalance(amount);
        balance.setShadowTotalBalance(BigDecimal.ZERO);
        balance.setShadowClearingBalance(BigDecimal.ZERO);
        balance.setInwardClearingBalance(BigDecimal.ZERO);
        balance.setUnclearedEffectBalance(BigDecimal.ZERO);
        balance.setLienBalance(BigDecimal.ZERO);
        balance.setChargeHoldBalance(BigDecimal.ZERO);
        balance.setHoldAmount(BigDecimal.ZERO);
        accountBalanceRepository.save(balance);

        account.setBalance(amount);
        accountRepository.save(account);
    }
}
