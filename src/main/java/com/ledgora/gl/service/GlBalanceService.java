package com.ledgora.gl.service;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PART 1: GL Balance Service - updates GL account balances following accounting sign conventions.
 *
 * <p>Accounting rules: - Debit increases Asset & Expense accounts - Credit increases Liability,
 * Income (Revenue), and Equity accounts
 *
 * <p>Propagates balance changes recursively to parent GL accounts.
 */
@Service
public class GlBalanceService {

    private static final Logger log = LoggerFactory.getLogger(GlBalanceService.class);
    private final GeneralLedgerRepository glRepository;

    public GlBalanceService(GeneralLedgerRepository glRepository) {
        this.glRepository = glRepository;
    }

    /**
     * Update GL account balance based on debit and credit amounts. Follows accounting sign
     * conventions and propagates to parent accounts.
     *
     * @param gl the GL account to update
     * @param debit debit amount for this posting
     * @param credit credit amount for this posting
     */
    @Transactional
    public void updateGlBalance(GeneralLedger gl, BigDecimal debit, BigDecimal credit) {
        if (gl == null) return;

        BigDecimal delta = calculateDelta(gl.getAccountType(), debit, credit);
        gl.setBalance(gl.getBalance().add(delta));
        glRepository.save(gl);

        log.debug(
                "GL {} ({}) balance updated by delta={}: new balance={}",
                gl.getGlCode(),
                gl.getAccountType(),
                delta,
                gl.getBalance());

        // Propagate delta to parent GL accounts recursively
        propagateToParent(gl.getParent(), debit, credit);
    }

    /**
     * Calculate balance delta based on GL account type and accounting conventions.
     *
     * <p>For ASSET and EXPENSE accounts (normal balance = DEBIT): Balance increases with debits,
     * decreases with credits Delta = debit - credit
     *
     * <p>For LIABILITY, REVENUE, and EQUITY accounts (normal balance = CREDIT): Balance increases
     * with credits, decreases with debits Delta = credit - debit
     */
    private BigDecimal calculateDelta(
            GLAccountType accountType, BigDecimal debit, BigDecimal credit) {
        return switch (accountType) {
            case ASSET, EXPENSE -> debit.subtract(credit);
            case LIABILITY, REVENUE, EQUITY -> credit.subtract(debit);
        };
    }

    /** Recursively propagate balance changes to parent GL accounts. Tenant-isolated. */
    private void propagateToParent(GeneralLedger parent, BigDecimal debit, BigDecimal credit) {
        if (parent == null) return;

        // Re-fetch to get latest state (tenant-isolated)
        Long tenantId =
                com.ledgora.tenant.context.TenantContextHolder.getRequiredTenantId();
        GeneralLedger parentGL =
                glRepository.findByIdAndTenantId(parent.getId(), tenantId).orElse(null);
        if (parentGL == null) return;

        BigDecimal delta = calculateDelta(parentGL.getAccountType(), debit, credit);
        parentGL.setBalance(parentGL.getBalance().add(delta));
        glRepository.save(parentGL);

        log.debug(
                "Parent GL {} ({}) balance updated by delta={}: new balance={}",
                parentGL.getGlCode(),
                parentGL.getAccountType(),
                delta,
                parentGL.getBalance());

        // Continue up the hierarchy
        propagateToParent(parentGL.getParent(), debit, credit);
    }
}
