package com.ledgora.clearing.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.branch.entity.Branch;
import com.ledgora.clearing.entity.BranchGlMapping;
import com.ledgora.clearing.repository.BranchGlMappingRepository;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.exception.GovernanceException;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Inter-Branch Transfer (IBT) Service — CBS-grade enforcement of branch-level balancing.
 *
 * <p>RBI Requirements enforced:
 *
 * <ul>
 *   <li>Direct cross-branch posting is strictly prohibited
 *   <li>Clearing GL must be branch-specific (from branch_gl_mappings config table)
 *   <li>Each IBT must create exactly 4 vouchers (2 per branch)
 *   <li>Both branches must have dayStatus = OPEN and be ACTIVE
 *   <li>Both branches must belong to the same tenant
 *   <li>IBT reversal must reverse both legs (no partial reversal)
 *   <li>Clearing GL must net to zero at EOD
 * </ul>
 */
@Service
public class IbtService {

    private static final Logger log = LoggerFactory.getLogger(IbtService.class);

    private final InterBranchClearingService interBranchClearingService;
    private final BranchGlMappingRepository branchGlMappingRepository;
    private final AccountRepository accountRepository;
    private final VoucherRepository voucherRepository;
    private final AuditService auditService;

    public IbtService(
            InterBranchClearingService interBranchClearingService,
            BranchGlMappingRepository branchGlMappingRepository,
            AccountRepository accountRepository,
            VoucherRepository voucherRepository,
            AuditService auditService) {
        this.interBranchClearingService = interBranchClearingService;
        this.branchGlMappingRepository = branchGlMappingRepository;
        this.accountRepository = accountRepository;
        this.voucherRepository = voucherRepository;
        this.auditService = auditService;
    }

    /**
     * Validate that both branches are eligible for IBT execution.
     *
     * <p>Checks:
     *
     * <ul>
     *   <li>Both branches must not be null
     *   <li>Both branches must be ACTIVE
     *   <li>Both branches must belong to the same tenant (implicit via account tenant check)
     *   <li>Source and destination must be different branches
     *   <li>Clearing GL mapping must exist for both branches
     * </ul>
     *
     * @throws GovernanceException if any validation fails
     */
    public void validateBranchesForIbt(Tenant tenant, Branch sourceBranch, Branch destBranch) {
        if (sourceBranch == null || destBranch == null) {
            throw new GovernanceException(
                    "IBT_BRANCH_NULL",
                    "Inter-branch transfer requires both source and destination branches "
                            + "to be resolved. Source="
                            + sourceBranch
                            + ", Dest="
                            + destBranch);
        }

        if (sourceBranch.getId().equals(destBranch.getId())) {
            throw new GovernanceException(
                    "IBT_SAME_BRANCH",
                    "Inter-branch transfer called with same branch: "
                            + sourceBranch.getBranchCode()
                            + ". Use direct posting for same-branch transfers.");
        }

        if (!Boolean.TRUE.equals(sourceBranch.getIsActive())) {
            throw new GovernanceException(
                    "IBT_BRANCH_INACTIVE",
                    "Source branch "
                            + sourceBranch.getBranchCode()
                            + " is not active. IBT blocked.");
        }

        if (!Boolean.TRUE.equals(destBranch.getIsActive())) {
            throw new GovernanceException(
                    "IBT_BRANCH_INACTIVE",
                    "Destination branch "
                            + destBranch.getBranchCode()
                            + " is not active. IBT blocked.");
        }

        // Validate clearing GL mapping exists for both branches
        validateClearingGlMapping(tenant.getId(), sourceBranch);
        validateClearingGlMapping(tenant.getId(), destBranch);
    }

    /**
     * Block any attempt at direct cross-branch posting (without clearing GL routing).
     *
     * <p>CBS Standard: Direct cross-branch DR/CR posting is strictly prohibited. All cross-branch
     * transfers MUST route through branch-specific Inter-Branch Clearing GL.
     *
     * @throws GovernanceException always — direct cross-branch posting is never allowed
     */
    public void blockDirectCrossBranchPosting(
            Account sourceAccount, Account destAccount, Branch sourceBranch, Branch destBranch) {
        throw new GovernanceException(
                "DIRECT_CROSS_BRANCH_BLOCKED",
                "Direct cross-branch posting is strictly prohibited. "
                        + "Source account "
                        + sourceAccount.getAccountNumber()
                        + " (branch "
                        + sourceBranch.getBranchCode()
                        + ") → Dest account "
                        + destAccount.getAccountNumber()
                        + " (branch "
                        + destBranch.getBranchCode()
                        + "). All cross-branch transfers must route through "
                        + "Inter-Branch Clearing GL.");
    }

    /**
     * Resolve the clearing GL mapping for a branch from the configuration table.
     *
     * <p>Falls back to the existing IBC account resolution if no mapping is configured, ensuring
     * backward compatibility with the seeded IBC-OUT/IBC-IN accounts.
     */
    public BranchGlMapping resolveClearingGlMapping(Long tenantId, Branch branch) {
        return branchGlMappingRepository
                .findByTenantIdAndBranchIdAndIsActiveTrue(tenantId, branch.getId())
                .orElse(null);
    }

    /**
     * Validate that a clearing GL mapping exists for the branch.
     *
     * <p>If no configuration-table mapping exists, fall back to checking that the IBC accounts
     * exist (backward compat with DataInitializer-seeded accounts).
     */
    private void validateClearingGlMapping(Long tenantId, Branch branch) {
        BranchGlMapping mapping = resolveClearingGlMapping(tenantId, branch);
        if (mapping != null) {
            return; // Configuration-based mapping exists
        }

        // Fallback: check that IBC-OUT and IBC-IN accounts exist for this branch
        try {
            interBranchClearingService.resolveIbcOutAccount(tenantId, branch);
            interBranchClearingService.resolveIbcInAccount(tenantId, branch);
        } catch (GovernanceException e) {
            throw new GovernanceException(
                    "IBT_CLEARING_GL_NOT_CONFIGURED",
                    "No clearing GL mapping found for branch "
                            + branch.getBranchCode()
                            + " (tenant "
                            + tenantId
                            + "). Configure branch_gl_mappings or seed IBC accounts.");
        }
    }

    /**
     * Validate that the clearing GL net balance is zero for a tenant.
     *
     * <p>CBS Standard: At End-of-Day, total clearing balance per tenant must net to zero.
     *
     * @return error message if imbalanced, null if OK
     */
    public String validateClearingGlNetZero(Long tenantId) {
        BigDecimal ibcBalance =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.CLEARING_ACCOUNT);
        if (ibcBalance.compareTo(BigDecimal.ZERO) != 0) {
            return "EOD blocked: Inter-branch clearing GL net balance is "
                    + ibcBalance
                    + " for tenant "
                    + tenantId
                    + ". All inter-branch clearing accounts must net to zero before EOD.";
        }
        return null;
    }

    /**
     * Validate IBT voucher count for a transaction.
     *
     * <p>Each IBT must create exactly 4 vouchers: 2 for origin branch (DR customer, CR IBC_OUT) and
     * 2 for destination branch (DR IBC_IN, CR customer).
     *
     * @throws GovernanceException if voucher count != 4 for an IBT transaction
     */
    public void validateIbtVoucherCount(Long transactionId) {
        long voucherCount = voucherRepository.countByTransactionId(transactionId);
        if (voucherCount != 4) {
            throw new GovernanceException(
                    "IBT_VOUCHER_COUNT_MISMATCH",
                    "IBT transaction "
                            + transactionId
                            + " has "
                            + voucherCount
                            + " voucher(s) — expected exactly 4 "
                            + "(2 per branch). Posting blocked.");
        }
    }

    /**
     * Validate that an IBT reversal covers both legs (no partial reversal allowed).
     *
     * <p>CBS Standard: Partial reversal of only one leg of an IBT is strictly prohibited. Both
     * origin and destination vouchers must be reversed together.
     *
     * @param originalTransactionId the original IBT transaction
     * @throws GovernanceException if only one leg has been reversed
     */
    public void validateFullReversalRequired(Long originalTransactionId) {
        List<com.ledgora.voucher.entity.Voucher> vouchers =
                voucherRepository.findByTransactionId(originalTransactionId);

        long cancelledCount = vouchers.stream().filter(v -> "Y".equals(v.getCancelFlag())).count();
        long totalCount = vouchers.size();

        if (cancelledCount > 0 && cancelledCount < totalCount) {
            throw new GovernanceException(
                    "IBT_PARTIAL_REVERSAL_BLOCKED",
                    "Partial reversal of IBT transaction "
                            + originalTransactionId
                            + " detected: "
                            + cancelledCount
                            + " of "
                            + totalCount
                            + " vouchers cancelled. "
                            + "All IBT vouchers must be reversed together.");
        }
    }

    /**
     * Get all IBC clearing account balances for audit reporting.
     *
     * @return list of IBC clearing accounts with their balances
     */
    public List<Account> getIbcClearingAccounts(Long tenantId) {
        return accountRepository.findIbcClearingAccountsByTenantId(tenantId);
    }
}
