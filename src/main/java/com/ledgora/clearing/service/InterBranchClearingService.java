package com.ledgora.clearing.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.clearing.repository.InterBranchTransferRepository;
import com.ledgora.common.enums.InterBranchTransferStatus;
import com.ledgora.tenant.entity.Tenant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inter-Branch Clearing Service — manages the lifecycle of cross-branch fund movements.
 *
 * <p>RBI Requirement: Branch books must independently balance.
 *
 * <p>Accounting model: Branch A (source): DR Customer A → CR IBC_OUT_A (Branch A balanced) Branch B
 * (dest): DR IBC_IN_B → CR Customer B (Branch B balanced) Settlement: DR IBC_IN → CR IBC_OUT
 * (Clearing zeroed)
 *
 * <p>IBC_OUT accounts use account number pattern: IBC-OUT-<branchCode> IBC_IN accounts use account
 * number pattern: IBC-IN-<branchCode>
 */
@Service
public class InterBranchClearingService {

    private static final Logger log = LoggerFactory.getLogger(InterBranchClearingService.class);

    private final InterBranchTransferRepository ibcRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public InterBranchClearingService(
            InterBranchTransferRepository ibcRepository,
            AccountRepository accountRepository,
            AuditService auditService) {
        this.ibcRepository = ibcRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    /** Detect whether a transfer is cross-branch. */
    public boolean isCrossBranch(Account sourceAccount, Account destAccount, User currentUser) {
        Branch sourceBranch = resolveBranch(sourceAccount, currentUser);
        Branch destBranch = resolveBranch(destAccount, currentUser);
        return sourceBranch != null
                && destBranch != null
                && !sourceBranch.getId().equals(destBranch.getId());
    }

    /** Create an InterBranchTransfer record in INITIATED status. */
    @Transactional
    public InterBranchTransfer createTransfer(
            Tenant tenant,
            Branch fromBranch,
            Branch toBranch,
            BigDecimal amount,
            String currency,
            com.ledgora.transaction.entity.Transaction referenceTransaction,
            LocalDate businessDate,
            User createdBy,
            String narration) {
        InterBranchTransfer transfer =
                InterBranchTransfer.builder()
                        .tenant(tenant)
                        .fromBranch(fromBranch)
                        .toBranch(toBranch)
                        .amount(amount)
                        .currency(currency)
                        .status(InterBranchTransferStatus.INITIATED)
                        .referenceTransaction(referenceTransaction)
                        .businessDate(businessDate)
                        .createdBy(createdBy)
                        .narration(narration)
                        .build();
        transfer = ibcRepository.save(transfer);

        log.info(
                "InterBranchTransfer created: id={} from={} to={} amount={} date={}",
                transfer.getId(),
                fromBranch.getBranchCode(),
                toBranch.getBranchCode(),
                amount,
                businessDate);

        auditService.logEvent(
                createdBy != null ? createdBy.getId() : null,
                "IBC_TRANSFER_CREATED",
                "INTER_BRANCH_TRANSFER",
                transfer.getId(),
                "IBC transfer "
                        + transfer.getId()
                        + ": "
                        + fromBranch.getBranchCode()
                        + " → "
                        + toBranch.getBranchCode()
                        + " amount="
                        + amount,
                null);

        return transfer;
    }

    /** Mark transfer as SENT (Branch A leg posted). Tenant-isolated. */
    @Transactional
    public void markSent(Long transferId) {
        InterBranchTransfer transfer = requireTransfer(transferId);
        transfer.setStatus(InterBranchTransferStatus.SENT);
        ibcRepository.save(transfer);
        log.info("InterBranchTransfer {} marked SENT", transferId);
    }

    /** Mark transfer as RECEIVED (Branch B leg posted). Tenant-isolated. */
    @Transactional
    public void markReceived(Long transferId) {
        InterBranchTransfer transfer = requireTransfer(transferId);
        if (transfer.getStatus() != InterBranchTransferStatus.SENT) {
            throw new RuntimeException(
                    "IBC transfer "
                            + transferId
                            + " cannot be marked RECEIVED — current status: "
                            + transfer.getStatus());
        }
        transfer.setStatus(InterBranchTransferStatus.RECEIVED);
        ibcRepository.save(transfer);
        log.info("InterBranchTransfer {} marked RECEIVED", transferId);
    }

    /** Mark transfer as FAILED with reason. Tenant-isolated. */
    @Transactional
    public void markFailed(Long transferId, String reason) {
        InterBranchTransfer transfer = requireTransfer(transferId);
        transfer.setStatus(InterBranchTransferStatus.FAILED);
        transfer.setFailureReason(reason);
        ibcRepository.save(transfer);
        log.warn("InterBranchTransfer {} marked FAILED: {}", transferId, reason);
    }

    /** Tenant-isolated lookup. Throws if not found or belongs to a different tenant. */
    private InterBranchTransfer requireTransfer(Long transferId) {
        Long tenantId = com.ledgora.tenant.context.TenantContextHolder.getRequiredTenantId();
        return ibcRepository
                .findByIdAndTenantId(transferId, tenantId)
                .orElseThrow(() -> new RuntimeException("IBC transfer not found: " + transferId));
    }

    /**
     * Resolve the IBC_OUT clearing account for a branch. Account number pattern:
     * IBC-OUT-<branchCode>
     */
    public Account resolveIbcOutAccount(Long tenantId, Branch branch) {
        String accountNumber = "IBC-OUT-" + branch.getBranchCode();
        return accountRepository
                .findByAccountNumberAndTenantId(accountNumber, tenantId)
                .orElseThrow(
                        () ->
                                new com.ledgora.common.exception.GovernanceException(
                                        "IBC_ACCOUNT_MISSING",
                                        "Inter-branch clearing account "
                                                + accountNumber
                                                + " not found for tenant "
                                                + tenantId
                                                + ". Seed IBC accounts in DataInitializer."));
    }

    /**
     * Resolve the IBC_IN clearing account for a branch. Account number pattern: IBC-IN-<branchCode>
     */
    public Account resolveIbcInAccount(Long tenantId, Branch branch) {
        String accountNumber = "IBC-IN-" + branch.getBranchCode();
        return accountRepository
                .findByAccountNumberAndTenantId(accountNumber, tenantId)
                .orElseThrow(
                        () ->
                                new com.ledgora.common.exception.GovernanceException(
                                        "IBC_ACCOUNT_MISSING",
                                        "Inter-branch clearing account "
                                                + accountNumber
                                                + " not found for tenant "
                                                + tenantId
                                                + ". Seed IBC accounts in DataInitializer."));
    }

    /** Get all SENT transfers pending receiving for a specific branch. */
    public List<InterBranchTransfer> getPendingReceiving(Long tenantId, Long toBranchId) {
        return ibcRepository.findByTenantIdAndToBranchIdAndStatus(
                tenantId, toBranchId, InterBranchTransferStatus.SENT);
    }

    /** Count unsettled transfers for EOD validation. */
    public long countUnsettled(Long tenantId, LocalDate businessDate) {
        return ibcRepository.countUnsettledByTenantAndDate(tenantId, businessDate);
    }

    /**
     * Settle all RECEIVED transfers for a business date. Called during settlement processing to
     * mark transfers as SETTLED.
     *
     * @return number of transfers settled
     */
    @Transactional
    public int settleTransfers(Long tenantId, LocalDate businessDate) {
        List<InterBranchTransfer> received =
                ibcRepository.findByTenantIdAndBusinessDateAndStatus(
                        tenantId, businessDate, InterBranchTransferStatus.RECEIVED);

        int count = 0;
        for (InterBranchTransfer transfer : received) {
            transfer.setStatus(InterBranchTransferStatus.SETTLED);
            transfer.setSettlementDate(businessDate);
            ibcRepository.save(transfer);
            count++;

            auditService.logEvent(
                    null,
                    "IBC_TRANSFER_SETTLED",
                    "INTER_BRANCH_TRANSFER",
                    transfer.getId(),
                    "IBC transfer "
                            + transfer.getId()
                            + " settled: "
                            + transfer.getFromBranch().getBranchCode()
                            + " → "
                            + transfer.getToBranch().getBranchCode()
                            + " amount="
                            + transfer.getAmount(),
                    null);
        }

        if (count > 0) {
            log.info(
                    "Settled {} inter-branch transfer(s) for tenant {} date {}",
                    count,
                    tenantId,
                    businessDate);
        }
        return count;
    }

    /** Validate clearing balance for EOD. Returns error message if imbalanced, null if OK. */
    public String validateClearingBalance(Long tenantId, LocalDate businessDate) {
        BigDecimal sentTotal = ibcRepository.sumSentAmountByTenantAndDate(tenantId, businessDate);
        BigDecimal receivedTotal =
                ibcRepository.sumReceivedAmountByTenantAndDate(tenantId, businessDate);

        long unsettled = ibcRepository.countUnsettledByTenantAndDate(tenantId, businessDate);
        if (unsettled > 0) {
            return "EOD blocked: "
                    + unsettled
                    + " unsettled inter-branch transfer(s) for "
                    + businessDate
                    + ". SENT total="
                    + sentTotal
                    + ", RECEIVED total="
                    + receivedTotal
                    + ". All transfers must be SETTLED or FAILED before EOD.";
        }
        return null;
    }

    private Branch resolveBranch(Account account, User currentUser) {
        if (account.getBranch() != null) return account.getBranch();
        if (account.getHomeBranch() != null) return account.getHomeBranch();
        if (currentUser != null && currentUser.getBranch() != null) return currentUser.getBranch();
        return null;
    }
}
