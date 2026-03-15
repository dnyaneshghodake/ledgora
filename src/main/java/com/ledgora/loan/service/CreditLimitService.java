package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.CreditLimit;
import com.ledgora.loan.repository.CreditLimitRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Credit Limit Service — CBS-grade credit facility management per RBI exposure norms.
 *
 * <p>Core operations:
 *
 * <ul>
 *   <li>Sanction a credit limit for a borrower (maker-checker)
 *   <li>Validate disbursement against available limit
 *   <li>Utilize limit on disbursement (reduce available)
 *   <li>Release limit on repayment/closure (increase available)
 *   <li>Check borrower/sector exposure caps
 * </ul>
 *
 * <p>RBI Master Circular on Exposure Norms:
 * Single borrower 15%, Group 40%, Sector per internal policy.
 */
@Service
public class CreditLimitService {

    private static final Logger log = LoggerFactory.getLogger(CreditLimitService.class);

    private final CreditLimitRepository creditLimitRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public CreditLimitService(
            CreditLimitRepository creditLimitRepository,
            TenantRepository tenantRepository,
            AuditService auditService) {
        this.creditLimitRepository = creditLimitRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    /**
     * Validate that a disbursement amount does not exceed the available credit limit.
     *
     * @throws BusinessException if limit is exceeded, expired, or not found
     */
    public void validateDisbursementAgainstLimit(Long creditLimitId, BigDecimal amount) {
        if (creditLimitId == null) {
            return; // No limit attached — skip validation (for backward compatibility)
        }
        CreditLimit limit =
                creditLimitRepository
                        .findById(creditLimitId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LIMIT_NOT_FOUND",
                                                "Credit limit not found: " + creditLimitId));

        if (!"ACTIVE".equals(limit.getStatus())) {
            throw new BusinessException(
                    "LIMIT_NOT_ACTIVE",
                    "Credit limit " + limit.getLimitReference() + " is " + limit.getStatus());
        }
        if (limit.getExpiryDate() != null
                && limit.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "LIMIT_EXPIRED",
                    "Credit limit " + limit.getLimitReference() + " expired on "
                            + limit.getExpiryDate());
        }
        if (amount.compareTo(limit.getAvailableAmount()) > 0) {
            throw new BusinessException(
                    "LIMIT_EXCEEDED",
                    "Disbursement "
                            + amount
                            + " exceeds available limit "
                            + limit.getAvailableAmount()
                            + " (sanctioned="
                            + limit.getSanctionedAmount()
                            + " utilized="
                            + limit.getUtilizedAmount()
                            + ")");
        }
    }

    /**
     * Utilize limit on successful disbursement.
     * Reduces available amount, increases utilized amount.
     */
    @Transactional
    public void utilizeLimit(Long creditLimitId, BigDecimal amount) {
        if (creditLimitId == null) return;
        CreditLimit limit =
                creditLimitRepository
                        .findById(creditLimitId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LIMIT_NOT_FOUND",
                                                "Credit limit not found: " + creditLimitId));

        limit.setUtilizedAmount(limit.getUtilizedAmount().add(amount));
        limit.setAvailableAmount(limit.getSanctionedAmount().subtract(limit.getUtilizedAmount()));
        creditLimitRepository.save(limit);

        log.info(
                "Limit utilized: ref={} amount={} newUtilized={} newAvailable={}",
                limit.getLimitReference(),
                amount,
                limit.getUtilizedAmount(),
                limit.getAvailableAmount());
    }

    /**
     * Release limit on repayment or loan closure.
     * Increases available amount, decreases utilized amount.
     */
    @Transactional
    public void releaseLimit(Long creditLimitId, BigDecimal amount) {
        if (creditLimitId == null) return;
        CreditLimit limit =
                creditLimitRepository
                        .findById(creditLimitId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LIMIT_NOT_FOUND",
                                                "Credit limit not found: " + creditLimitId));

        BigDecimal newUtilized = limit.getUtilizedAmount().subtract(amount);
        if (newUtilized.compareTo(BigDecimal.ZERO) < 0) {
            newUtilized = BigDecimal.ZERO;
        }
        limit.setUtilizedAmount(newUtilized);
        limit.setAvailableAmount(limit.getSanctionedAmount().subtract(newUtilized));
        creditLimitRepository.save(limit);

        log.info(
                "Limit released: ref={} amount={} newUtilized={} newAvailable={}",
                limit.getLimitReference(),
                amount,
                limit.getUtilizedAmount(),
                limit.getAvailableAmount());
    }

    /** Get all active limits for a tenant. */
    public List<CreditLimit> getActiveLimits(Long tenantId) {
        return creditLimitRepository.findActiveByTenantId(tenantId);
    }

    /** Get total exposure for a borrower. */
    public BigDecimal getBorrowerExposure(Long tenantId, String borrowerId) {
        return creditLimitRepository.sumUtilizedByBorrower(tenantId, borrowerId);
    }
}
