package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.dto.LoanRateDTO;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.entity.LoanRate;
import com.ledgora.loan.entity.LoanRateChangeHistory;
import com.ledgora.loan.enums.InterestType;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanProductRepository;
import com.ledgora.loan.repository.LoanRateChangeHistoryRepository;
import com.ledgora.loan.repository.LoanRateRepository;
import com.ledgora.loan.validation.EmiCalculator;
import com.ledgora.tenant.context.TenantContextHolder;
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
 * Loan Rate Service — Finacle LARATE + RBI FPC compliant rate management.
 *
 * <p>Capabilities:
 *
 * <ul>
 *   <li>Create new rate for a product with effective date
 *   <li>Maintain active/inactive rate window per product
 *   <li>Write immutable rate-change history entries for audit
 *   <li>Propagate floating rate changes to active loan accounts (EMI recalculation) — limited to
 *       updating loan.interestRate + loan.emiAmount (schedule restructuring is a separate step)
 * </ul>
 */
@Service
public class LoanRateService {

    private static final Logger log = LoggerFactory.getLogger(LoanRateService.class);

    private final TenantRepository tenantRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanRateRepository loanRateRepository;
    private final LoanRateChangeHistoryRepository historyRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final AuditService auditService;

    public LoanRateService(
            TenantRepository tenantRepository,
            LoanProductRepository loanProductRepository,
            LoanRateRepository loanRateRepository,
            LoanRateChangeHistoryRepository historyRepository,
            LoanAccountRepository loanAccountRepository,
            AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.loanProductRepository = loanProductRepository;
        this.loanRateRepository = loanRateRepository;
        this.historyRepository = historyRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new loan rate for a product.
     *
     * <p>Rules:
     *
     * <ul>
     *   <li>Tenant isolation enforced
     *   <li>Effective date must be provided
     *   <li>For FIXED products: effectiveRate must be provided
     *   <li>For FLOATING products: benchmarkRate + spread determines effectiveRate
     *   <li>Closes previous active rate window by setting endDate = effectiveDate.minusDays(1)
     * </ul>
     */
    @Transactional
    public LoanRate createRate(Long tenantId, LoanRateDTO dto) {
        if (dto == null) {
            throw new BusinessException("INVALID_RATE", "Rate payload is required");
        }
        if (dto.getProductId() == null) {
            throw new BusinessException("INVALID_RATE", "ProductId is required");
        }
        if (dto.getEffectiveDate() == null) {
            throw new BusinessException("INVALID_RATE", "Effective date is required");
        }

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "TENANT_NOT_FOUND", "Tenant not found"));

        LoanProduct product =
                loanProductRepository
                        .findById(dto.getProductId())
                        .filter(p -> p.getTenant().getId().equals(tenantId))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "PRODUCT_NOT_FOUND",
                                                "Loan product not found for this tenant"));

        // Prevent duplicate rate for same effective date
        loanRateRepository
                .findByLoanProductIdAndEffectiveDate(product.getId(), dto.getEffectiveDate())
                .ifPresent(
                        r -> {
                            throw new BusinessException(
                                    "RATE_EXISTS",
                                    "Rate already exists for product "
                                            + product.getProductCode()
                                            + " effective "
                                            + dto.getEffectiveDate());
                        });

        InterestType interestType = product.getInterestType();

        BigDecimal spread = dto.getSpread() != null ? dto.getSpread() : BigDecimal.ZERO;
        BigDecimal benchmarkRate = dto.getBenchmarkRate();
        BigDecimal effectiveRate;

        if (interestType == InterestType.FIXED) {
            if (dto.getEffectiveRate() == null) {
                throw new BusinessException(
                        "INVALID_RATE", "effectiveRate is required for FIXED products");
            }
            effectiveRate = dto.getEffectiveRate();
        } else {
            if (dto.getBenchmarkName() == null || dto.getBenchmarkName().isBlank()) {
                throw new BusinessException(
                        "INVALID_RATE", "benchmarkName is required for FLOATING products");
            }
            if (benchmarkRate == null) {
                throw new BusinessException(
                        "INVALID_RATE", "benchmarkRate is required for FLOATING products");
            }
            effectiveRate = benchmarkRate.add(spread);
        }

        // Close current active rate (if any)
        List<LoanRate> activeRates = loanRateRepository.findActiveByProductId(product.getId());
        LoanRate previous = activeRates.isEmpty() ? null : activeRates.get(0);
        if (previous != null) {
            previous.setIsActive(false);
            previous.setEndDate(dto.getEffectiveDate().minusDays(1));
            loanRateRepository.save(previous);
        }

        LoanRate newRate =
                LoanRate.builder()
                        .tenant(tenant)
                        .loanProduct(product)
                        .benchmarkName(dto.getBenchmarkName())
                        .benchmarkRate(benchmarkRate)
                        .spread(spread)
                        .effectiveRate(effectiveRate)
                        .interestType(interestType)
                        .effectiveDate(dto.getEffectiveDate())
                        .endDate(null)
                        .isActive(true)
                        .remarks(dto.getRemarks())
                        .build();
        newRate = loanRateRepository.save(newRate);

        // Update product interestRate for backward compatibility with existing code
        product.setInterestRate(effectiveRate);
        loanProductRepository.save(product);

        // Audit trail  borrower-dispute safe
        String changedBy = TenantContextHolder.getUsername();
        LoanRateChangeHistory history =
                LoanRateChangeHistory.builder()
                        .tenant(tenant)
                        .loanProduct(product)
                        .loanAccount(null)
                        .loanRate(newRate)
                        .oldRate(previous != null ? previous.getEffectiveRate() : BigDecimal.ZERO)
                        .newRate(effectiveRate)
                        .oldBenchmarkRate(previous != null ? previous.getBenchmarkRate() : null)
                        .newBenchmarkRate(benchmarkRate)
                        .oldEmi(null)
                        .newEmi(null)
                        .effectiveDate(dto.getEffectiveDate())
                        .changeReason(
                                dto.getChangeReason() != null
                                        ? dto.getChangeReason()
                                        : "PRODUCT_REVISION")
                        .remarks(dto.getRemarks())
                        .changedBy(changedBy)
                        .build();
        historyRepository.save(history);

        auditService.logEvent(
                null,
                "LOAN_RATE_CHANGED",
                "LOAN_PRODUCT",
                product.getId(),
                "Loan rate changed for product "
                        + product.getProductCode()
                        + " old="
                        + (previous != null ? previous.getEffectiveRate() : "N/A")
                        + " new="
                        + effectiveRate
                        + " effective="
                        + dto.getEffectiveDate(),
                null);

        log.info(
                "Loan rate changed: tenant={} product={} old={} new={} effectiveDate={}",
                tenantId,
                product.getProductCode(),
                previous != null ? previous.getEffectiveRate() : null,
                effectiveRate,
                dto.getEffectiveDate());

        return newRate;
    }

    /**
     * Propagate a floating rate change to active loans of a product.
     *
     * <p>Finacle supports various rescheduling methods; this is the minimal CBS-grade step: update
     * loan-level interestRate + recompute stored emiAmount. Full schedule regeneration is handled
     * by LoanRestructureService (future).
     */
    @Transactional
    public int propagateRateToActiveLoans(Long tenantId, Long productId, LocalDate asOfDate) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "TENANT_NOT_FOUND", "Tenant not found"));

        LoanProduct product =
                loanProductRepository
                        .findById(productId)
                        .filter(p -> p.getTenant().getId().equals(tenantId))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "PRODUCT_NOT_FOUND",
                                                "Loan product not found for this tenant"));

        if (product.getInterestType() != InterestType.FLOATING) {
            throw new BusinessException(
                    "NOT_FLOATING", "Rate propagation only applies to FLOATING products");
        }

        LocalDate effectiveDate = asOfDate != null ? asOfDate : tenant.getCurrentBusinessDate();
        List<LoanRate> rates =
                loanRateRepository.findEffectiveByProductIdAndDate(productId, effectiveDate);
        if (rates.isEmpty()) {
            throw new BusinessException(
                    "RATE_NOT_FOUND",
                    "No rate found for product "
                            + product.getProductCode()
                            + " as of "
                            + effectiveDate);
        }
        LoanRate rate = rates.get(0);

        List<LoanAccount> loans =
                loanAccountRepository.findByTenantIdAndStatus(
                        tenantId, com.ledgora.loan.enums.LoanStatus.ACTIVE);
        int updated = 0;
        for (LoanAccount loan : loans) {
            if (loan.getLoanProduct() == null || !loan.getLoanProduct().getId().equals(productId)) {
                continue;
            }
            BigDecimal oldRate = loan.getInterestRate();
            BigDecimal oldEmi = loan.getEmiAmount();

            loan.setInterestRate(rate.getEffectiveRate());
            // CBS: Compute remaining tenure from unpaid schedule installments.
            // Using the product's full tenure would produce incorrect EMI for
            // partially repaid loans — the EMI must be based on the remaining
            // number of installments, not the original tenure.
            long remainingInstallments =
                    loanAccountRepository.countRemainingInstallments(loan.getId());
            int remainingMonths =
                    remainingInstallments > 0
                            ? (int) remainingInstallments
                            : loan.getLoanProduct().getTenureMonths();
            loan.setEmiAmount(
                    EmiCalculator.computeEmi(
                            loan.getOutstandingPrincipal(),
                            rate.getEffectiveRate(),
                            remainingMonths));
            loanAccountRepository.save(loan);

            historyRepository.save(
                    LoanRateChangeHistory.builder()
                            .tenant(tenant)
                            .loanProduct(product)
                            .loanAccount(loan)
                            .loanRate(rate)
                            .oldRate(oldRate != null ? oldRate : BigDecimal.ZERO)
                            .newRate(rate.getEffectiveRate())
                            .oldBenchmarkRate(null)
                            .newBenchmarkRate(rate.getBenchmarkRate())
                            .oldEmi(oldEmi)
                            .newEmi(loan.getEmiAmount())
                            .effectiveDate(effectiveDate)
                            .changeReason("BENCHMARK_RESET")
                            .remarks("Auto-propagated floating rate change")
                            .changedBy(TenantContextHolder.getUsername())
                            .build());

            updated++;
        }

        log.info(
                "Floating rate propagated: tenant={} product={} updatedLoans={} effectiveDate={}",
                tenantId,
                product.getProductCode(),
                updated,
                effectiveDate);

        return updated;
    }
}
