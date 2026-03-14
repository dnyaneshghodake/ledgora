package com.ledgora.config.seeder;

import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.tenant.entity.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 2 — Branch (SOL) seeding. Seeds HQ001, BR001, BR002 with Finacle-grade
 * branchName + tenant linkage.
 */
@Component
public class BranchDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(BranchDataSeeder.class);
    private final BranchRepository branchRepository;

    public BranchDataSeeder(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public Branch seedBranch(
            String branchCode,
            String branchName,
            String address,
            String city,
            String state,
            String pincode,
            String ifscCode,
            String micrCode,
            String branchType,
            String contactPhone,
            Tenant tenant) {
        return branchRepository
                .findByBranchCode(branchCode)
                .orElseGet(
                        () -> {
                            Branch b =
                                    Branch.builder()
                                            .branchCode(branchCode)
                                            .branchName(branchName)
                                            .name(branchName)
                                            .address(address)
                                            .city(city)
                                            .state(state)
                                            .pincode(pincode)
                                            .ifscCode(ifscCode)
                                            .micrCode(micrCode)
                                            .branchType(branchType)
                                            .contactPhone(contactPhone)
                                            .tenant(tenant)
                                            .isActive(true)
                                            .build();
                            return branchRepository.save(b);
                        });
    }

    /** Seeds standard CBS branches with full RBI attributes. Returns [hqBranch, branch1, branch2]. */
    public Branch[] seed(Tenant defaultTenant) {
        Branch hq =
                seedBranch(
                        "HQ001",
                        "Head Office",
                        "Main Street, City Center",
                        "Mumbai",
                        "Maharashtra",
                        "400001",
                        "LDGR0000001",
                        "400002001",
                        "HEAD_OFFICE",
                        "022-40001001",
                        defaultTenant);
        Branch br1 =
                seedBranch(
                        "BR001",
                        "Downtown Branch",
                        "1st Avenue, Downtown",
                        "Mumbai",
                        "Maharashtra",
                        "400002",
                        "LDGR0000002",
                        "400002002",
                        "BRANCH",
                        "022-40001002",
                        defaultTenant);
        Branch br2 =
                seedBranch(
                        "BR002",
                        "Uptown Branch",
                        "2nd Avenue, Uptown",
                        "Delhi",
                        "Delhi",
                        "110001",
                        "LDGR0000003",
                        "110002001",
                        "BRANCH",
                        "011-40001003",
                        defaultTenant);
        log.info("  [Branches] HQ001, BR001, BR002 ready (with IFSC, MICR, branchType)");
        return new Branch[] {hq, br1, br2};
    }
}
