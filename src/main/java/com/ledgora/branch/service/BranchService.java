package com.ledgora.branch.service;

import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
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

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Transactional
    public Branch createBranch(String branchCode, String name, String address) {
        if (branchRepository.existsByBranchCode(branchCode)) {
            throw new RuntimeException("Branch code already exists: " + branchCode);
        }
        Branch branch =
                Branch.builder()
                        .branchCode(branchCode)
                        .name(name)
                        .address(address)
                        .isActive(true)
                        .build();
        Branch saved = branchRepository.save(branch);
        log.info("Branch created: {} - {}", saved.getBranchCode(), saved.getName());
        return saved;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Optional<Branch> getBranchByCode(String code) {
        return branchRepository.findByBranchCode(code);
    }

    public Optional<Branch> getBranchById(Long id) {
        return branchRepository.findById(id);
    }
}
