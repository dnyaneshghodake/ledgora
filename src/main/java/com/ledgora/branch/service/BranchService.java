package com.ledgora.branch.service;

import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {

    private static final Logger log = LoggerFactory.getLogger(BranchService.class);
    private final BranchRepository branchRepository;
    private final TenantService tenantService;

    public BranchService(BranchRepository branchRepository, TenantService tenantService) {
        this.branchRepository = branchRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public Branch createBranch(String branchCode, String branchName, String address) {
        if (branchRepository.existsByBranchCode(branchCode)) {
            throw new RuntimeException("Branch code already exists: " + branchCode);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant = tenantService.getTenantById(tenantId);
        Branch branch =
                Branch.builder()
                        .branchCode(branchCode)
                        .branchName(branchName)
                        .name(branchName)
                        .address(address)
                        .tenant(tenant)
                        .isActive(true)
                        .build();
        Branch saved = branchRepository.save(branch);
        log.info("Branch created: {} - {}", saved.getBranchCode(), saved.getBranchName());
        return saved;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public List<Branch> getActiveBranchesByTenant(Long tenantId) {
        return branchRepository.findActiveByTenantId(tenantId);
    }

    public List<Branch> getBranchesByTenant(Long tenantId) {
        return branchRepository.findByTenantId(tenantId);
    }

    public Optional<Branch> getBranchByCode(String code) {
        return branchRepository.findByBranchCode(code);
    }

    public Optional<Branch> getBranchById(Long id) {
        return branchRepository.findById(id);
    }

    public Optional<Branch> getBranchByTenantAndCode(Long tenantId, String branchCode) {
        return branchRepository.findByTenantIdAndBranchCode(tenantId, branchCode);
    }
}
