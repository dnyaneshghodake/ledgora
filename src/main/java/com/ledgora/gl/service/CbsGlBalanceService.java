package com.ledgora.gl.service;

import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.entity.GlBranchBalance;
import com.ledgora.gl.entity.GlTenantBalance;
import com.ledgora.gl.repository.GlBranchBalanceRepository;
import com.ledgora.gl.repository.GlTenantBalanceRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS GL Balance Service - manages GL balances at branch and tenant level.
 *
 * <p>On voucher post: If DR: Debit account, Credit GL If CR: Credit account, Debit GL
 *
 * <p>Updates: - gl_actual_balance (on GlBranchBalance) - branch_gl_balance (aggregate per branch) -
 * tenant_gl_balance (aggregate per tenant)
 *
 * <p>GL must be tenant + branch scoped.
 */
@Service
public class CbsGlBalanceService {

    private static final Logger log = LoggerFactory.getLogger(CbsGlBalanceService.class);

    private final GlBranchBalanceRepository branchBalanceRepository;
    private final GlTenantBalanceRepository tenantBalanceRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;

    public CbsGlBalanceService(
            GlBranchBalanceRepository branchBalanceRepository,
            GlTenantBalanceRepository tenantBalanceRepository,
            TenantRepository tenantRepository,
            BranchRepository branchRepository) {
        this.branchBalanceRepository = branchBalanceRepository;
        this.tenantBalanceRepository = tenantBalanceRepository;
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
    }

    /** Update GL balance at branch and tenant level. Called on voucher posting. */
    @Transactional
    public void updateGlBalances(Long tenantId, Long branchId, GeneralLedger gl, BigDecimal delta) {
        if (gl == null) return;

        // Update branch-level GL balance
        updateBranchGlBalance(tenantId, branchId, gl, delta);

        // Update tenant-level GL balance
        updateTenantGlBalance(tenantId, gl, delta);

        log.debug(
                "CBS GL balances updated: tenant={}, branch={}, gl={}, delta={}",
                tenantId,
                branchId,
                gl.getGlCode(),
                delta);
    }

    /** Update branch-level GL balance. */
    @Transactional
    public void updateBranchGlBalance(
            Long tenantId, Long branchId, GeneralLedger gl, BigDecimal delta) {
        GlBranchBalance branchBalance =
                branchBalanceRepository
                        .findByTenantIdAndBranchIdAndGlIdWithLock(tenantId, branchId, gl.getId())
                        .orElseGet(
                                () -> {
                                    Tenant tenant =
                                            tenantRepository
                                                    .findById(tenantId)
                                                    .orElseThrow(
                                                            () ->
                                                                    new RuntimeException(
                                                                            "Tenant not found: "
                                                                                    + tenantId));
                                    Branch branch =
                                            branchRepository
                                                    .findById(branchId)
                                                    .orElseThrow(
                                                            () ->
                                                                    new RuntimeException(
                                                                            "Branch not found: "
                                                                                    + branchId));
                                    return GlBranchBalance.builder()
                                            .tenant(tenant)
                                            .branch(branch)
                                            .gl(gl)
                                            .glActualBalance(BigDecimal.ZERO)
                                            .build();
                                });

        branchBalance.setGlActualBalance(branchBalance.getGlActualBalance().add(delta));
        branchBalanceRepository.save(branchBalance);
    }

    /** Update tenant-level GL balance. */
    @Transactional
    public void updateTenantGlBalance(Long tenantId, GeneralLedger gl, BigDecimal delta) {
        GlTenantBalance tenantBalance =
                tenantBalanceRepository
                        .findByTenantIdAndGlIdWithLock(tenantId, gl.getId())
                        .orElseGet(
                                () -> {
                                    Tenant tenant =
                                            tenantRepository
                                                    .findById(tenantId)
                                                    .orElseThrow(
                                                            () ->
                                                                    new RuntimeException(
                                                                            "Tenant not found: "
                                                                                    + tenantId));
                                    return GlTenantBalance.builder()
                                            .tenant(tenant)
                                            .gl(gl)
                                            .glActualBalance(BigDecimal.ZERO)
                                            .build();
                                });

        tenantBalance.setGlActualBalance(tenantBalance.getGlActualBalance().add(delta));
        tenantBalanceRepository.save(tenantBalance);
    }

    /** Check if branch GL is balanced (sum of all GL balances at branch level should be zero). */
    public boolean isBranchGlBalanced(Long tenantId, Long branchId) {
        BigDecimal sum =
                branchBalanceRepository.sumBalanceByTenantIdAndBranchId(tenantId, branchId);
        return sum.compareTo(BigDecimal.ZERO) == 0;
    }

    /** Check if tenant GL is balanced (sum of all GL balances at tenant level should be zero). */
    public boolean isTenantGlBalanced(Long tenantId) {
        BigDecimal sum = tenantBalanceRepository.sumBalanceByTenantId(tenantId);
        return sum.compareTo(BigDecimal.ZERO) == 0;
    }
}
