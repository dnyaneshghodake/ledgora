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

    /** Seeds standard CBS branches for Tenant 1. Returns [hqBranch, branch1, branch2]. */
    public Branch[] seed(Tenant defaultTenant) {
        Branch hq =
                seedBranch(
                        "HQ001",
                        "Head Office",
                        "Nariman Point, Fort Area",
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
                        "Andheri West Branch",
                        "Link Road, Andheri West",
                        "Mumbai",
                        "Maharashtra",
                        "400058",
                        "LDGR0000002",
                        "400002002",
                        "BRANCH",
                        "022-40001002",
                        defaultTenant);
        Branch br2 =
                seedBranch(
                        "BR002",
                        "Connaught Place Branch",
                        "Block A, Inner Circle, CP",
                        "New Delhi",
                        "Delhi",
                        "110001",
                        "LDGR0000003",
                        "110002001",
                        "BRANCH",
                        "011-40001003",
                        defaultTenant);
        log.info("  [Branches] T1: HQ001, BR001, BR002 ready");
        return new Branch[] {hq, br1, br2};
    }

    /** Seeds branches for Tenant 2 — Partner Bank (2 branches). */
    public Branch[] seedForTenant2(Tenant tenant) {
        Branch hq =
                seedBranch(
                        "T2-HQ01",
                        "Partner Bank Head Office",
                        "MG Road, Camp Area",
                        "Pune",
                        "Maharashtra",
                        "411001",
                        "LDGR0100001",
                        "411002001",
                        "HEAD_OFFICE",
                        "020-40002001",
                        tenant);
        Branch br1 =
                seedBranch(
                        "T2-BR01",
                        "Hinjewadi IT Park Branch",
                        "Phase 1, Hinjewadi",
                        "Pune",
                        "Maharashtra",
                        "411057",
                        "LDGR0100002",
                        "411002002",
                        "BRANCH",
                        "020-40002002",
                        tenant);
        Branch br2 =
                seedBranch(
                        "T2-BR02",
                        "Kothrud Branch",
                        "Karve Road, Kothrud",
                        "Pune",
                        "Maharashtra",
                        "411038",
                        "LDGR0100003",
                        "411002003",
                        "BRANCH",
                        "020-40002003",
                        tenant);
        log.info("  [Branches] T2: T2-HQ01, T2-BR01, T2-BR02 ready");
        return new Branch[] {hq, br1, br2};
    }

    /** Seeds branches for Tenant 3 — Sahyadri UCB (single branch per UCB norms). */
    public Branch[] seedForTenant3(Tenant tenant) {
        Branch hq =
                seedBranch(
                        "T3-HQ01",
                        "Sahyadri UCB Main Office",
                        "Tilak Road, Sadashiv Peth",
                        "Pune",
                        "Maharashtra",
                        "411030",
                        "SUBC0000001",
                        "411003001",
                        "HEAD_OFFICE",
                        "020-40003001",
                        tenant);
        Branch br1 =
                seedBranch(
                        "T3-BR01",
                        "Sahyadri UCB Deccan Branch",
                        "FC Road, Deccan Gymkhana",
                        "Pune",
                        "Maharashtra",
                        "411004",
                        "SUBC0000002",
                        "411003002",
                        "BRANCH",
                        "020-40003002",
                        tenant);
        log.info("  [Branches] T3: T3-HQ01, T3-BR01 ready (UCB — limited branches)");
        return new Branch[] {hq, br1};
    }

    /** Seeds branches for Tenant 4 — Maharashtra Gramin Bank (rural branches). */
    public Branch[] seedForTenant4(Tenant tenant) {
        Branch hq =
                seedBranch(
                        "T4-HQ01",
                        "Gramin Bank Regional Office",
                        "Station Road",
                        "Nashik",
                        "Maharashtra",
                        "422001",
                        "MGRB0000001",
                        "422004001",
                        "HEAD_OFFICE",
                        "0253-4001001",
                        tenant);
        Branch br1 =
                seedBranch(
                        "T4-BR01",
                        "Sinnar Taluka Branch",
                        "Market Yard Road, Sinnar",
                        "Sinnar",
                        "Maharashtra",
                        "422103",
                        "MGRB0000002",
                        "422004002",
                        "BRANCH",
                        "02551-4001002",
                        tenant);
        Branch br2 =
                seedBranch(
                        "T4-BR02",
                        "Igatpuri Branch",
                        "Mumbai-Agra Highway, Igatpuri",
                        "Igatpuri",
                        "Maharashtra",
                        "422403",
                        "MGRB0000003",
                        "422004003",
                        "BRANCH",
                        "02553-4001003",
                        tenant);
        Branch br3 =
                seedBranch(
                        "T4-BR03",
                        "Dindori Branch",
                        "Dindori Taluka, Nashik District",
                        "Dindori",
                        "Maharashtra",
                        "422202",
                        "MGRB0000004",
                        "422004004",
                        "BRANCH",
                        "02557-4001004",
                        tenant);
        log.info("  [Branches] T4: T4-HQ01, T4-BR01..BR03 ready (RRB — rural branches)");
        return new Branch[] {hq, br1, br2, br3};
    }

    /** Seeds branches for Tenant 5 — Finserv Capital NBFC (metro + digital). */
    public Branch[] seedForTenant5(Tenant tenant) {
        Branch hq =
                seedBranch(
                        "T5-HQ01",
                        "Finserv Capital Corporate Office",
                        "BKC, Bandra East",
                        "Mumbai",
                        "Maharashtra",
                        "400051",
                        "FNCL0000001",
                        "400005001",
                        "HEAD_OFFICE",
                        "022-40005001",
                        tenant);
        Branch br1 =
                seedBranch(
                        "T5-BR01",
                        "Finserv Bengaluru Hub",
                        "Koramangala, 4th Block",
                        "Bengaluru",
                        "Karnataka",
                        "560034",
                        "FNCL0000002",
                        "560005002",
                        "BRANCH",
                        "080-40005002",
                        tenant);
        Branch br2 =
                seedBranch(
                        "T5-BR02",
                        "Finserv Delhi NCR Hub",
                        "Sector 44, Gurugram",
                        "Gurugram",
                        "Haryana",
                        "122003",
                        "FNCL0000003",
                        "122005003",
                        "BRANCH",
                        "0124-40005003",
                        tenant);
        Branch br3 =
                seedBranch(
                        "T5-BR03",
                        "Finserv Hyderabad Hub",
                        "HITEC City, Madhapur",
                        "Hyderabad",
                        "Telangana",
                        "500081",
                        "FNCL0000004",
                        "500005004",
                        "BRANCH",
                        "040-40005004",
                        tenant);
        log.info("  [Branches] T5: T5-HQ01, T5-BR01..BR03 ready (NBFC — metro hubs)");
        return new Branch[] {hq, br1, br2, br3};
    }
}
