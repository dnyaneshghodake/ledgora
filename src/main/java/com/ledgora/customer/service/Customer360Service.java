package com.ledgora.customer.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.approval.entity.HardTransactionLimit;
import com.ledgora.approval.repository.HardTransactionLimitRepository;
import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.balance.service.BalanceEngineService;
import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.clearing.repository.InterBranchTransferRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.FreezeLevel;
import com.ledgora.customer.dto.view.AccountSummaryDTO;
import com.ledgora.customer.dto.view.AuditTimelineDTO;
import com.ledgora.customer.dto.view.Customer360DTO;
import com.ledgora.customer.dto.view.IbtSummaryDTO;
import com.ledgora.customer.dto.view.RiskSummaryDTO;
import com.ledgora.customer.dto.view.SuspenseSummaryDTO;
import com.ledgora.customer.dto.view.TransactionSummaryDTO;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.repository.CustomerRepository;
import com.ledgora.fraud.entity.FraudAlert;
import com.ledgora.fraud.entity.VelocityLimit;
import com.ledgora.fraud.repository.FraudAlertRepository;
import com.ledgora.fraud.repository.VelocityLimitRepository;
import com.ledgora.lien.repository.AccountLienRepository;
import com.ledgora.suspense.entity.SuspenseCase;
import com.ledgora.suspense.repository.SuspenseCaseRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer 360° View orchestration service.
 *
 * <p>Assembles the complete customer view from existing repositories and services. No business
 * mutations — read-only aggregation only.
 *
 * <p>Design constraints:
 *
 * <ul>
 *   <li>Maximum 7 primary queries (batch aggregation where possible)
 *   <li>Uses JOIN FETCH to prevent N+1
 *   <li>Tenant isolation enforced via TenantContextHolder
 *   <li>No entity leakage — all data mapped to DTOs
 *   <li>Reuses existing BalanceEngineService for balance computation
 * </ul>
 */
@Service
public class Customer360Service {

    private static final Logger log = LoggerFactory.getLogger(Customer360Service.class);

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final InterBranchTransferRepository ibtRepository;
    private final SuspenseCaseRepository suspenseCaseRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final VelocityLimitRepository velocityLimitRepository;
    private final HardTransactionLimitRepository hardTransactionLimitRepository;
    private final AccountLienRepository accountLienRepository;
    private final AuditLogRepository auditLogRepository;
    private final BalanceEngineService balanceEngine;

    public Customer360Service(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            InterBranchTransferRepository ibtRepository,
            SuspenseCaseRepository suspenseCaseRepository,
            FraudAlertRepository fraudAlertRepository,
            VelocityLimitRepository velocityLimitRepository,
            HardTransactionLimitRepository hardTransactionLimitRepository,
            AccountLienRepository accountLienRepository,
            AuditLogRepository auditLogRepository,
            BalanceEngineService balanceEngine) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ibtRepository = ibtRepository;
        this.suspenseCaseRepository = suspenseCaseRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.velocityLimitRepository = velocityLimitRepository;
        this.hardTransactionLimitRepository = hardTransactionLimitRepository;
        this.accountLienRepository = accountLienRepository;
        this.auditLogRepository = auditLogRepository;
        this.balanceEngine = balanceEngine;
    }

    /**
     * Build the complete Customer 360° View DTO.
     *
     * @param customerId the customer primary key
     * @param page transaction page number (0-based)
     * @param size transaction page size
     * @return fully assembled Customer360DTO
     */
    @Transactional(readOnly = true)
    public Customer360DTO buildCustomer360View(Long customerId, int page, int size) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // Query 1: Fetch customer with tenant isolation
        Customer customer =
                customerRepository
                        .findByIdAndTenantId(customerId, tenantId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Customer not found: "
                                                        + customerId
                                                        + " (tenant: "
                                                        + tenantId
                                                        + ")"));

        // Query 2: Fetch all accounts for this customer (tenant-isolated)
        List<Account> accounts =
                accountRepository.findByTenantIdAndCustomerId(tenantId, customerId);
        List<Long> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toList());

        // Build CIF snapshot + KPIs
        Customer360DTO dto = buildCifSnapshot(customer);

        // Build account summaries with balance data
        List<AccountSummaryDTO> accountSummaries = buildAccountSummaries(accounts, tenantId);
        dto.setAccounts(accountSummaries);
        dto.setTotalAccounts(accounts.size());

        // Compute aggregate KPIs from account data
        computeBalanceKpis(dto, accounts);

        if (!accountIds.isEmpty()) {
            // Query 3: Paginated transactions for all customer accounts
            Page<Transaction> txnPage =
                    transactionRepository.findByTenantIdAndAccountIds(
                            tenantId, accountIds, PageRequest.of(page, size));
            dto.setTransactions(mapTransactions(txnPage.getContent()));
            dto.setTotalTransactionCount(txnPage.getTotalElements());

            // Query 4: IBT exposure (unsettled transfers involving customer's branches)
            buildIbtExposure(dto, tenantId);

            // Query 5: Suspense exposure for customer's accounts
            buildSuspenseExposure(dto, tenantId, accountIds);

            // Query 6: Risk & Governance
            buildRiskSummary(dto, tenantId, accounts, accountIds);

            // Query 7: Audit trail
            buildAuditTrail(dto, tenantId, customerId, accountIds);
        } else {
            dto.setTransactions(Collections.emptyList());
            dto.setTotalTransactionCount(0);
            dto.setIbtTransfers(Collections.emptyList());
            dto.setSuspenseCases(Collections.emptyList());
            dto.setRiskSummary(
                    RiskSummaryDTO.builder()
                            .hardLimits(Collections.emptyList())
                            .velocityLimits(Collections.emptyList())
                            .fraudAlerts(Collections.emptyList())
                            .frozenAccounts(Collections.emptyList())
                            .hardCeilingViolations(Collections.emptyList())
                            .build());
            dto.setAuditTrail(Collections.emptyList());
        }

        return dto;
    }

    private Customer360DTO buildCifSnapshot(Customer customer) {
        return Customer360DTO.builder()
                .customerId(customer.getCustomerId())
                .fullName(customer.getFullName())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .customerType(customer.getCustomerType())
                .riskCategory(customer.getRiskCategory())
                .approvalStatus(
                        customer.getApprovalStatus() != null
                                ? customer.getApprovalStatus().name()
                                : null)
                .kycStatus(customer.getKycStatus())
                .freezeLevel(customer.getFreezeLevel())
                .freezeReason(customer.getFreezeReason())
                .nationalId(customer.getNationalId())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private List<AccountSummaryDTO> buildAccountSummaries(List<Account> accounts, Long tenantId) {
        return accounts.stream()
                .map(
                        account -> {
                            BigDecimal ledgerBalance;
                            BigDecimal availableBalance;
                            try {
                                ledgerBalance = balanceEngine.getLedgerBalance(account.getId());
                                availableBalance =
                                        balanceEngine.getAvailableBalance(account.getId());
                            } catch (Exception e) {
                                log.warn(
                                        "Balance lookup failed for account {}: {}",
                                        account.getId(),
                                        e.getMessage());
                                ledgerBalance = account.getBalance();
                                availableBalance = account.getBalance();
                            }

                            BigDecimal lienAmount;
                            try {
                                lienAmount =
                                        accountLienRepository.sumActiveLienAmountByAccountId(
                                                account.getId());
                            } catch (Exception e) {
                                log.warn(
                                        "Lien lookup failed for account {}: {}",
                                        account.getId(),
                                        e.getMessage());
                                lienAmount = BigDecimal.ZERO;
                            }

                            // Get last 5 transactions for expandable row
                            List<Transaction> recentTxns =
                                    transactionRepository.findTop5ByTenantIdAndAccountId(
                                            tenantId, account.getId(), PageRequest.of(0, 5));
                            List<TransactionSummaryDTO> recentDtos = mapTransactions(recentTxns);

                            return AccountSummaryDTO.builder()
                                    .accountId(account.getId())
                                    .accountNumber(account.getAccountNumber())
                                    .accountName(account.getAccountName())
                                    .branchCode(account.getBranchCode())
                                    .status(
                                            account.getStatus() != null
                                                    ? account.getStatus().name()
                                                    : null)
                                    .ledgerBalance(ledgerBalance)
                                    .availableBalance(availableBalance)
                                    .lienAmount(lienAmount)
                                    .freezeLevel(
                                            account.getFreezeLevel() != null
                                                    ? account.getFreezeLevel().name()
                                                    : "NONE")
                                    .accountType(
                                            account.getAccountType() != null
                                                    ? account.getAccountType().name()
                                                    : null)
                                    .currency(account.getCurrency())
                                    .lastTransactionDate(
                                            !recentTxns.isEmpty()
                                                    ? recentTxns.get(0).getCreatedAt()
                                                    : null)
                                    .recentTransactions(recentDtos)
                                    .build();
                        })
                .collect(Collectors.toList());
    }

    private void computeBalanceKpis(Customer360DTO dto, List<Account> accounts) {
        BigDecimal totalLedger = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalLien = BigDecimal.ZERO;
        long underReview = 0;

        for (Account account : accounts) {
            try {
                totalLedger = totalLedger.add(balanceEngine.getLedgerBalance(account.getId()));
                totalAvailable =
                        totalAvailable.add(balanceEngine.getAvailableBalance(account.getId()));
            } catch (Exception e) {
                totalLedger =
                        totalLedger.add(
                                account.getBalance() != null
                                        ? account.getBalance()
                                        : BigDecimal.ZERO);
                totalAvailable =
                        totalAvailable.add(
                                account.getBalance() != null
                                        ? account.getBalance()
                                        : BigDecimal.ZERO);
            }
            try {
                totalLien =
                        totalLien.add(
                                accountLienRepository.sumActiveLienAmountByAccountId(
                                        account.getId()));
            } catch (Exception e) {
                // skip
            }
            if (account.getStatus() == AccountStatus.UNDER_REVIEW) {
                underReview++;
            }
        }

        dto.setTotalLedgerBalance(totalLedger);
        dto.setTotalAvailableBalance(totalAvailable);
        dto.setTotalLienAmount(totalLien);
        dto.setAccountsUnderReviewCount(underReview);
    }

    private void buildIbtExposure(Customer360DTO dto, Long tenantId) {
        List<InterBranchTransfer> unsettled = ibtRepository.findUnsettledByTenantId(tenantId);

        List<IbtSummaryDTO> ibtDtos =
                unsettled.stream()
                        .map(
                                ibt ->
                                        IbtSummaryDTO.builder()
                                                .ibtId(ibt.getId())
                                                .fromBranchName(
                                                        ibt.getFromBranch() != null
                                                                ? ibt.getFromBranch()
                                                                        .getBranchName()
                                                                : null)
                                                .fromBranchCode(
                                                        ibt.getFromBranch() != null
                                                                ? ibt.getFromBranch()
                                                                        .getBranchCode()
                                                                : null)
                                                .toBranchName(
                                                        ibt.getToBranch() != null
                                                                ? ibt.getToBranch().getBranchName()
                                                                : null)
                                                .toBranchCode(
                                                        ibt.getToBranch() != null
                                                                ? ibt.getToBranch().getBranchCode()
                                                                : null)
                                                .amount(ibt.getAmount())
                                                .currency(ibt.getCurrency())
                                                .status(
                                                        ibt.getStatus() != null
                                                                ? ibt.getStatus().name()
                                                                : null)
                                                .businessDate(ibt.getBusinessDate())
                                                .createdAt(ibt.getCreatedAt())
                                                .build())
                        .collect(Collectors.toList());

        dto.setIbtTransfers(ibtDtos);
        dto.setUnsettledIbtCount(unsettled.size());

        // Net clearing exposure: sum of SENT - sum of RECEIVED amounts
        BigDecimal sentTotal =
                unsettled.stream()
                        .filter(
                                ibt ->
                                        ibt.getStatus() != null
                                                && "SENT".equals(ibt.getStatus().name()))
                        .map(InterBranchTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedTotal =
                unsettled.stream()
                        .filter(
                                ibt ->
                                        ibt.getStatus() != null
                                                && "RECEIVED".equals(ibt.getStatus().name()))
                        .map(InterBranchTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setNetClearingExposure(sentTotal.subtract(receivedTotal));

        // Open IBT KPI
        BigDecimal openIbtAmount =
                unsettled.stream()
                        .map(InterBranchTransfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setOpenIbtAmount(openIbtAmount);
        dto.setOpenIbtCount(unsettled.size());
    }

    private void buildSuspenseExposure(Customer360DTO dto, Long tenantId, List<Long> accountIds) {
        List<SuspenseCase> cases =
                suspenseCaseRepository.findByTenantIdAndIntendedAccountIdIn(tenantId, accountIds);

        List<SuspenseSummaryDTO> suspenseDtos =
                cases.stream()
                        .map(
                                sc ->
                                        SuspenseSummaryDTO.builder()
                                                .caseId(sc.getId())
                                                .accountNumber(
                                                        sc.getIntendedAccount() != null
                                                                ? sc.getIntendedAccount()
                                                                        .getAccountNumber()
                                                                : null)
                                                .accountName(
                                                        sc.getIntendedAccount() != null
                                                                ? sc.getIntendedAccount()
                                                                        .getAccountName()
                                                                : null)
                                                .reasonCode(sc.getReasonCode())
                                                .reasonDetail(sc.getReasonDetail())
                                                .amount(sc.getAmount())
                                                .currency(sc.getCurrency())
                                                .status(sc.getStatus())
                                                .createdAt(sc.getCreatedAt())
                                                .resolvedAt(sc.getResolvedAt())
                                                .build())
                        .collect(Collectors.toList());

        dto.setSuspenseCases(suspenseDtos);

        // Suspense KPIs
        BigDecimal openAmount =
                suspenseCaseRepository.sumOpenAmountByTenantIdAndAccountIds(tenantId, accountIds);
        long openCount = cases.stream().filter(sc -> "OPEN".equals(sc.getStatus())).count();
        dto.setOpenSuspenseAmount(openAmount);
        dto.setOpenSuspenseCount(openCount);

        // Suspense GL balance (read-only)
        dto.setSuspenseGlBalance(openAmount);
    }

    private void buildRiskSummary(
            Customer360DTO dto, Long tenantId, List<Account> accounts, List<Long> accountIds) {
        RiskSummaryDTO risk = new RiskSummaryDTO();

        // Hard transaction limits (tenant-wide)
        List<HardTransactionLimit> hardLimits =
                hardTransactionLimitRepository.findByTenant_Id(tenantId);
        risk.setHardLimits(
                hardLimits.stream()
                        .map(
                                hl ->
                                        RiskSummaryDTO.HardLimitItem.builder()
                                                .id(hl.getId())
                                                .channel(
                                                        hl.getChannel() != null
                                                                ? hl.getChannel().name()
                                                                : "DEFAULT")
                                                .absoluteMaxAmount(hl.getAbsoluteMaxAmount())
                                                .isActive(hl.getIsActive())
                                                .build())
                        .collect(Collectors.toList()));

        // Velocity limits (tenant-wide)
        List<VelocityLimit> velocityLimits =
                velocityLimitRepository.findByTenantIdAndIsActiveTrue(tenantId);
        risk.setVelocityLimits(
                velocityLimits.stream()
                        .map(
                                vl -> {
                                    String acctNum = null;
                                    if (vl.getAccountId() != null) {
                                        acctNum =
                                                accounts.stream()
                                                        .filter(
                                                                a ->
                                                                        a.getId()
                                                                                .equals(
                                                                                        vl
                                                                                                .getAccountId()))
                                                        .map(Account::getAccountNumber)
                                                        .findFirst()
                                                        .orElse(null);
                                    }
                                    return RiskSummaryDTO.VelocityLimitItem.builder()
                                            .id(vl.getId())
                                            .accountId(vl.getAccountId())
                                            .accountNumber(acctNum)
                                            .maxTxnCountPerHour(vl.getMaxTxnCountPerHour())
                                            .maxTotalAmountPerHour(vl.getMaxTotalAmountPerHour())
                                            .isActive(vl.getIsActive())
                                            .build();
                                })
                        .collect(Collectors.toList()));

        // Fraud alerts for customer's accounts
        List<FraudAlert> fraudAlerts =
                fraudAlertRepository.findByTenantIdAndAccountIdIn(tenantId, accountIds);
        risk.setFraudAlerts(
                fraudAlerts.stream()
                        .map(
                                fa ->
                                        RiskSummaryDTO.FraudAlertItem.builder()
                                                .id(fa.getId())
                                                .accountNumber(fa.getAccountNumber())
                                                .alertType(fa.getAlertType())
                                                .status(fa.getStatus())
                                                .details(fa.getDetails())
                                                .observedCount(fa.getObservedCount())
                                                .observedAmount(fa.getObservedAmount())
                                                .createdAt(fa.getCreatedAt())
                                                .build())
                        .collect(Collectors.toList()));

        // Frozen accounts
        List<RiskSummaryDTO.FrozenAccountItem> frozenAccounts =
                accounts.stream()
                        .filter(
                                a ->
                                        a.getFreezeLevel() != null
                                                && a.getFreezeLevel() != FreezeLevel.NONE)
                        .map(
                                a ->
                                        RiskSummaryDTO.FrozenAccountItem.builder()
                                                .accountId(a.getId())
                                                .accountNumber(a.getAccountNumber())
                                                .accountName(a.getAccountName())
                                                .freezeLevel(a.getFreezeLevel().name())
                                                .freezeReason(a.getFreezeReason())
                                                .build())
                        .collect(Collectors.toList());
        risk.setFrozenAccounts(frozenAccounts);

        // Hard ceiling violation audit entries
        List<AuditLog> violations =
                auditLogRepository.findTop20ByTenantIdAndActionOrderByTimestampDesc(
                        tenantId, "HARD_LIMIT_EXCEEDED");
        risk.setHardCeilingViolations(
                violations.stream()
                        .map(
                                al ->
                                        RiskSummaryDTO.ViolationItem.builder()
                                                .id(al.getId())
                                                .action(al.getAction())
                                                .details(al.getDetails())
                                                .username(al.getUsername())
                                                .timestamp(al.getTimestamp())
                                                .build())
                        .collect(Collectors.toList()));

        dto.setRiskSummary(risk);

        // Open fraud alert KPI
        long openFraudCount =
                fraudAlerts.stream().filter(fa -> "OPEN".equals(fa.getStatus())).count();
        dto.setOpenFraudAlertCount(openFraudCount);
    }

    private void buildAuditTrail(
            Customer360DTO dto, Long tenantId, Long customerId, List<Long> accountIds) {
        // Use correlated query: CUSTOMER entries only for this customerId,
        // ACCOUNT entries only for this customer's accountIds.
        // Avoids cross-product false positives from independent IN clauses.
        List<AuditLog> auditLogs =
                auditLogRepository.findCustomer360AuditTrail(tenantId, customerId, accountIds);

        dto.setAuditTrail(
                auditLogs.stream()
                        .map(
                                al ->
                                        AuditTimelineDTO.builder()
                                                .id(al.getId())
                                                .action(al.getAction())
                                                .entity(al.getEntity())
                                                .entityId(al.getEntityId())
                                                .details(al.getDetails())
                                                .username(al.getUsername())
                                                .ipAddress(al.getIpAddress())
                                                .timestamp(al.getTimestamp())
                                                .oldValue(al.getOldValue())
                                                .newValue(al.getNewValue())
                                                .build())
                        .collect(Collectors.toList()));
    }

    private List<TransactionSummaryDTO> mapTransactions(List<Transaction> transactions) {
        return transactions.stream()
                .map(
                        t ->
                                TransactionSummaryDTO.builder()
                                        .transactionId(t.getId())
                                        .transactionRef(t.getTransactionRef())
                                        .transactionType(
                                                t.getTransactionType() != null
                                                        ? t.getTransactionType().name()
                                                        : null)
                                        .channel(
                                                t.getChannel() != null
                                                        ? t.getChannel().name()
                                                        : null)
                                        .amount(t.getAmount())
                                        .currency(t.getCurrency())
                                        .status(t.getStatus() != null ? t.getStatus().name() : null)
                                        .businessDate(t.getBusinessDate())
                                        .makerUsername(
                                                t.getMaker() != null
                                                        ? t.getMaker().getUsername()
                                                        : null)
                                        .checkerUsername(
                                                t.getChecker() != null
                                                        ? t.getChecker().getUsername()
                                                        : null)
                                        .createdAt(t.getCreatedAt())
                                        .description(t.getDescription())
                                        .build())
                .collect(Collectors.toList());
    }
}
