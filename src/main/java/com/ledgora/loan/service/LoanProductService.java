package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.repository.LoanProductRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loan Product Service — Finacle product master lifecycle management.
 *
 * <p>Per RBI Master Directions on Interest Rate on Advances:
 *
 * <ul>
 *   <li>Product defines interest rate, tenure, compounding, penalty, NPA threshold
 *   <li>GL mappings drive all accounting entries through the voucher engine
 *   <li>Products are tenant-scoped — no cross-tenant access
 *   <li>Deactivation (soft-delete) only — no hard delete of products with active loans
 * </ul>
 */
@Service
public class LoanProductService {

    private static final Logger log = LoggerFactory.getLogger(LoanProductService.class);

    private final LoanProductRepository loanProductRepository;
    private final AuditService auditService;

    public LoanProductService(
            LoanProductRepository loanProductRepository, AuditService auditService) {
        this.loanProductRepository = loanProductRepository;
        this.auditService = auditService;
    }

    /** Get all active products for a tenant. */
    public List<LoanProduct> getActiveProducts(Long tenantId) {
        return loanProductRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    /** Get all products for a tenant (including inactive). */
    public List<LoanProduct> getAllProducts(Long tenantId) {
        return loanProductRepository.findByTenantId(tenantId);
    }

    /** Get product by ID with tenant isolation. */
    public LoanProduct getProduct(Long productId, Long tenantId) {
        return loanProductRepository
                .findById(productId)
                .filter(p -> p.getTenant().getId().equals(tenantId))
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "PRODUCT_NOT_FOUND",
                                        "Loan product not found for this tenant"));
    }

    /** Deactivate a product (soft-delete). */
    @Transactional
    public LoanProduct deactivateProduct(Long productId, Long tenantId) {
        LoanProduct product = getProduct(productId, tenantId);
        if (!product.getIsActive()) {
            throw new BusinessException("PRODUCT_ALREADY_INACTIVE", "Product is already inactive");
        }
        product.setIsActive(false);
        product = loanProductRepository.save(product);

        auditService.logEvent(
                null,
                "LOAN_PRODUCT_DEACTIVATED",
                "LOAN_PRODUCT",
                product.getId(),
                "Product " + product.getProductCode() + " deactivated",
                null);

        log.info("Loan product deactivated: {}", product.getProductCode());
        return product;
    }

    /** Reactivate a product. */
    @Transactional
    public LoanProduct reactivateProduct(Long productId, Long tenantId) {
        LoanProduct product = getProduct(productId, tenantId);
        if (product.getIsActive()) {
            throw new BusinessException("PRODUCT_ALREADY_ACTIVE", "Product is already active");
        }
        product.setIsActive(true);
        product = loanProductRepository.save(product);

        auditService.logEvent(
                null,
                "LOAN_PRODUCT_REACTIVATED",
                "LOAN_PRODUCT",
                product.getId(),
                "Product " + product.getProductCode() + " reactivated",
                null);

        log.info("Loan product reactivated: {}", product.getProductCode());
        return product;
    }
}
