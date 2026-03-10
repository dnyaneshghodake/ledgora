package com.ledgora.suspense.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.exception.GovernanceException;
import com.ledgora.suspense.entity.SuspenseCase;
import com.ledgora.suspense.entity.SuspenseGlMapping;
import com.ledgora.suspense.repository.SuspenseCaseRepository;
import com.ledgora.suspense.repository.SuspenseGlMappingRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.voucher.entity.Voucher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suspense GL Resolution Service — CBS-grade exception accounting.
 *
 * <p>Handles:
 *
 * <ul>
 *   <li>Resolving the suspense account for a tenant+channel
 *   <li>Creating SuspenseCase records when partial posting failures occur
 *   <li>Resolving suspense cases (retry credit posting or reverse debit)
 *   <li>Enforcing maker-checker on resolution
 *   <li>EOD validation: suspense balance tolerance check
 *   <li>Micrometer metric emission
 * </ul>
 */
@Service
public class SuspenseResolutionService {

    private static final Logger log = LoggerFactory.getLogger(SuspenseResolutionService.class);

    private final SuspenseCaseRepository suspenseCaseRepository;
    private final SuspenseGlMappingRepository suspenseGlMappingRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final Counter suspenseCreatedCounter;

    public SuspenseResolutionService(
            SuspenseCaseRepository suspenseCaseRepository,
            SuspenseGlMappingRepository suspenseGlMappingRepository,
            AccountRepository accountRepository,
            AuditService auditService,
            MeterRegistry meterRegistry) {
        this.suspenseCaseRepository = suspenseCaseRepository;
        this.suspenseGlMappingRepository = suspenseGlMappingRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
        this.suspenseCreatedCounter =
                Counter.builder("ledgora.suspense.created")
                        .description("Total suspense cases created due to partial posting failure")
                        .register(meterRegistry);
    }

    /**
     * Resolve the suspense account for a tenant and channel.
     *
     * <p>Resolution order: 1. Channel-specific mapping from suspense_gl_mappings 2. Default
     * (channel=null) mapping from suspense_gl_mappings 3. Fallback: find any SUSPENSE_ACCOUNT or
     * INTERNAL_ACCOUNT with "SUSP" in account number
     *
     * @throws GovernanceException if no suspense account is configured
     */
    public Account resolveSuspenseAccount(Long tenantId, TransactionChannel channel) {
        // 1. Try channel-specific mapping
        if (channel != null) {
            SuspenseGlMapping channelMapping =
                    suspenseGlMappingRepository
                            .findByTenantIdAndChannelAndIsActiveTrue(tenantId, channel)
                            .orElse(null);
            if (channelMapping != null) {
                return lookupSuspenseAccount(tenantId, channelMapping.getSuspenseAccountNumber());
            }
        }

        // 2. Try default (channel=null) mapping
        SuspenseGlMapping defaultMapping =
                suspenseGlMappingRepository
                        .findByTenantIdAndChannelIsNullAndIsActiveTrue(tenantId)
                        .orElse(null);
        if (defaultMapping != null) {
            return lookupSuspenseAccount(tenantId, defaultMapping.getSuspenseAccountNumber());
        }

        // 3. Fallback: find any SUSPENSE_ACCOUNT type for the tenant
        return accountRepository
                .findByTenantIdAndAccountType(tenantId, AccountType.SUSPENSE_ACCOUNT)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new GovernanceException(
                                        "SUSPENSE_ACCOUNT_MISSING",
                                        "No suspense account configured for tenant "
                                                + tenantId
                                                + ". Seed a SUSPENSE_ACCOUNT or configure"
                                                + " suspense_gl_mappings."));
    }

    private Account lookupSuspenseAccount(Long tenantId, String accountNumber) {
        return accountRepository
                .findByAccountNumberAndTenantId(accountNumber, tenantId)
                .orElseThrow(
                        () ->
                                new GovernanceException(
                                        "SUSPENSE_ACCOUNT_MISSING",
                                        "Suspense account "
                                                + accountNumber
                                                + " not found for tenant "
                                                + tenantId
                                                + "."));
    }

    /**
     * Create a SuspenseCase record when a partial posting failure occurs.
     *
     * <p>Called by the posting engine after the debit leg succeeds but the credit leg fails. The
     * credit leg has already been re-posted to the suspense account by the caller.
     */
    @Transactional
    public SuspenseCase createSuspenseCase(
            Tenant tenant,
            Transaction transaction,
            Voucher postedVoucher,
            Voucher suspenseVoucher,
            Account intendedAccount,
            Account suspenseAccount,
            BigDecimal amount,
            String currency,
            String reasonCode,
            String reasonDetail,
            LocalDate businessDate) {

        SuspenseCase suspenseCase =
                SuspenseCase.builder()
                        .tenant(tenant)
                        .originalTransaction(transaction)
                        .postedVoucher(postedVoucher)
                        .suspenseVoucher(suspenseVoucher)
                        .intendedAccount(intendedAccount)
                        .suspenseAccount(suspenseAccount)
                        .amount(amount)
                        .currency(currency)
                        .reasonCode(reasonCode)
                        .reasonDetail(reasonDetail)
                        .status("OPEN")
                        .businessDate(businessDate)
                        .build();
        suspenseCase = suspenseCaseRepository.save(suspenseCase);

        // Emit metric
        suspenseCreatedCounter.increment();

        // Audit trail
        auditService.logEvent(
                null,
                "SUSPENSE_CASE_CREATED",
                "SUSPENSE_CASE",
                suspenseCase.getId(),
                "Suspense case "
                        + suspenseCase.getId()
                        + " created: txn="
                        + transaction.getTransactionRef()
                        + " amount="
                        + amount
                        + " reason="
                        + reasonCode
                        + " intended="
                        + intendedAccount.getAccountNumber()
                        + " suspense="
                        + suspenseAccount.getAccountNumber(),
                null);

        log.warn(
                "SUSPENSE CASE CREATED: id={} txn={} amount={} reason={} intended={} suspense={}",
                suspenseCase.getId(),
                transaction.getTransactionRef(),
                amount,
                reasonCode,
                intendedAccount.getAccountNumber(),
                suspenseAccount.getAccountNumber());

        return suspenseCase;
    }

    /**
     * Resolve a suspense case by marking it as RESOLVED after retry/correction succeeds.
     *
     * <p>Enforces maker-checker: resolver must differ from the maker of the original transaction.
     */
    @Transactional
    public SuspenseCase resolveCase(
            Long caseId,
            User resolver,
            User checker,
            Voucher resolutionVoucher,
            String remarks) {

        SuspenseCase sc =
                suspenseCaseRepository
                        .findById(caseId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Suspense case not found: " + caseId));

        if (!"OPEN".equals(sc.getStatus())) {
            throw new RuntimeException(
                    "Suspense case " + caseId + " is already " + sc.getStatus());
        }

        // Maker-checker enforcement
        if (resolver != null && checker != null && resolver.getId().equals(checker.getId())) {
            throw new GovernanceException(
                    "SUSPENSE_MAKER_CHECKER_VIOLATION",
                    "Suspense resolution requires different maker and checker. "
                            + "Resolver="
                            + resolver.getUsername()
                            + " cannot also be checker.");
        }

        sc.setStatus("RESOLVED");
        sc.setResolvedBy(resolver);
        sc.setResolutionChecker(checker);
        sc.setResolutionVoucher(resolutionVoucher);
        sc.setResolutionRemarks(remarks);
        sc.setResolvedAt(LocalDateTime.now());
        sc = suspenseCaseRepository.save(sc);

        auditService.logEvent(
                resolver != null ? resolver.getId() : null,
                "SUSPENSE_CASE_RESOLVED",
                "SUSPENSE_CASE",
                sc.getId(),
                "Suspense case "
                        + sc.getId()
                        + " resolved: "
                        + remarks
                        + " resolver="
                        + (resolver != null ? resolver.getUsername() : "N/A")
                        + " checker="
                        + (checker != null ? checker.getUsername() : "N/A"),
                null);

        log.info("Suspense case {} resolved by {}", caseId, resolver != null ? resolver.getUsername() : "N/A");
        return sc;
    }

    /**
     * Reverse a suspense case by marking it as REVERSED after the debit leg is also reversed.
     */
    @Transactional
    public SuspenseCase reverseCase(Long caseId, User resolver, User checker, String remarks) {
        SuspenseCase sc =
                suspenseCaseRepository
                        .findById(caseId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Suspense case not found: " + caseId));

        if (!"OPEN".equals(sc.getStatus())) {
            throw new RuntimeException(
                    "Suspense case " + caseId + " is already " + sc.getStatus());
        }

        if (resolver != null && checker != null && resolver.getId().equals(checker.getId())) {
            throw new GovernanceException(
                    "SUSPENSE_MAKER_CHECKER_VIOLATION",
                    "Suspense reversal requires different maker and checker.");
        }

        sc.setStatus("REVERSED");
        sc.setResolvedBy(resolver);
        sc.setResolutionChecker(checker);
        sc.setResolutionRemarks(remarks);
        sc.setResolvedAt(LocalDateTime.now());
        sc = suspenseCaseRepository.save(sc);

        auditService.logEvent(
                resolver != null ? resolver.getId() : null,
                "SUSPENSE_CASE_REVERSED",
                "SUSPENSE_CASE",
                sc.getId(),
                "Suspense case " + sc.getId() + " reversed: " + remarks,
                null);

        log.info("Suspense case {} reversed by {}", caseId, resolver != null ? resolver.getUsername() : "N/A");
        return sc;
    }

    /** Get all open suspense cases for a tenant. */
    public List<SuspenseCase> getOpenCases(Long tenantId) {
        return suspenseCaseRepository.findByTenantIdAndStatus(tenantId, "OPEN");
    }

    /** Count open suspense cases for a tenant. */
    public long countOpenCases(Long tenantId) {
        return suspenseCaseRepository.countOpenByTenantId(tenantId);
    }

    /**
     * EOD validation: check if suspense balance exceeds configured tolerance.
     *
     * @param toleranceAmount maximum allowed open suspense amount (0 = zero tolerance)
     * @return error message if exceeded, null if OK
     */
    public String validateSuspenseForEod(Long tenantId, BigDecimal toleranceAmount) {
        BigDecimal openAmount = suspenseCaseRepository.sumOpenAmountByTenantId(tenantId);
        long openCount = suspenseCaseRepository.countOpenByTenantId(tenantId);

        if (openCount > 0 && openAmount.compareTo(toleranceAmount) > 0) {
            return "EOD blocked: "
                    + openCount
                    + " open suspense case(s) with total amount "
                    + openAmount
                    + " exceeds tolerance threshold "
                    + toleranceAmount
                    + " for tenant "
                    + tenantId
                    + ". Resolve or reverse all suspense cases before EOD.";
        }
        return null;
    }

    /**
     * Validate suspense account balance is zero (direct account-level check).
     *
     * @return error message if non-zero, null if OK
     */
    public String validateSuspenseAccountBalance(Long tenantId) {
        BigDecimal suspenseBalance =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.SUSPENSE_ACCOUNT);
        if (suspenseBalance.compareTo(BigDecimal.ZERO) != 0) {
            return "EOD blocked: Suspense GL balance is "
                    + suspenseBalance
                    + " for tenant "
                    + tenantId
                    + ". All suspense entries must be cleared before EOD.";
        }
        return null;
    }
}
