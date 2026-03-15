package com.ledgora.config.seeder;

import com.ledgora.common.enums.DayStatus;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 0 — Tenant seeding. Seeds default and secondary tenants for multi-tenancy.
 */
@Component
public class TenantDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TenantDataSeeder.class);
    private final TenantRepository tenantRepository;

    public TenantDataSeeder(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant seedDefaultTenant() {
        return tenantRepository
                .findByTenantCode("TENANT-001")
                .orElseGet(
                        () -> {
                            Tenant t =
                                    Tenant.builder()
                                            .tenantCode("TENANT-001")
                                            .tenantName("Ledgora Main Bank")
                                            .status("ACTIVE")
                                            .currentBusinessDate(SeederDateUtil.nextWeekday())
                                            .dayStatus(DayStatus.OPEN)
                                            .country("IN")
                                            .baseCurrency("INR")
                                            .timezone("Asia/Kolkata")
                                            .regulatoryCode("RBI/2024/BANK/001")
                                            .multiBranchEnabled(true)
                                            .eodStatus("NOT_STARTED")
                                            .effectiveFrom(java.time.LocalDate.of(2024, 1, 1))
                                            .remarks("Primary tenant — seeded by DataInitializer")
                                            .build();
                            return tenantRepository.save(t);
                        });
    }

    public Tenant seedSecondTenant() {
        return tenantRepository
                .findByTenantCode("TENANT-002")
                .orElseGet(
                        () -> {
                            Tenant t =
                                    Tenant.builder()
                                            .tenantCode("TENANT-002")
                                            .tenantName("Ledgora Partner Bank")
                                            .status("ACTIVE")
                                            .currentBusinessDate(SeederDateUtil.nextWeekday())
                                            .dayStatus(DayStatus.OPEN)
                                            .country("IN")
                                            .baseCurrency("INR")
                                            .timezone("Asia/Kolkata")
                                            .regulatoryCode("RBI/2024/BANK/002")
                                            .multiBranchEnabled(false)
                                            .eodStatus("NOT_STARTED")
                                            .effectiveFrom(java.time.LocalDate.of(2024, 6, 1))
                                            .remarks(
                                                    "Secondary tenant — partner bank for multi-tenant testing")
                                            .build();
                            return tenantRepository.save(t);
                        });
    }

    /** Tenant 3: Cooperative Bank — single-branch, rural banking. */
    public Tenant seedCooperativeBank() {
        return tenantRepository
                .findByTenantCode("TENANT-003")
                .orElseGet(
                        () -> {
                            Tenant t =
                                    Tenant.builder()
                                            .tenantCode("TENANT-003")
                                            .tenantName("Sahyadri Urban Cooperative Bank")
                                            .status("ACTIVE")
                                            .currentBusinessDate(SeederDateUtil.nextWeekday())
                                            .dayStatus(DayStatus.OPEN)
                                            .country("IN")
                                            .baseCurrency("INR")
                                            .timezone("Asia/Kolkata")
                                            .regulatoryCode("RBI/2024/UCB/003")
                                            .multiBranchEnabled(false)
                                            .eodStatus("NOT_STARTED")
                                            .effectiveFrom(java.time.LocalDate.of(2023, 4, 1))
                                            .remarks(
                                                    "Urban cooperative bank — RBI Tier-II UCB")
                                            .build();
                            return tenantRepository.save(t);
                        });
    }

    /** Tenant 4: Regional Rural Bank — government-sponsored, priority sector lending. */
    public Tenant seedRuralBank() {
        return tenantRepository
                .findByTenantCode("TENANT-004")
                .orElseGet(
                        () -> {
                            Tenant t =
                                    Tenant.builder()
                                            .tenantCode("TENANT-004")
                                            .tenantName("Maharashtra Gramin Bank")
                                            .status("ACTIVE")
                                            .currentBusinessDate(SeederDateUtil.nextWeekday())
                                            .dayStatus(DayStatus.OPEN)
                                            .country("IN")
                                            .baseCurrency("INR")
                                            .timezone("Asia/Kolkata")
                                            .regulatoryCode("RBI/2024/RRB/004")
                                            .multiBranchEnabled(true)
                                            .eodStatus("NOT_STARTED")
                                            .effectiveFrom(java.time.LocalDate.of(2022, 1, 1))
                                            .remarks(
                                                    "Regional Rural Bank — priority sector lending, NABARD sponsored")
                                            .build();
                            return tenantRepository.save(t);
                        });
    }

    /** Tenant 5: NBFC — non-banking financial company, lending-focused. */
    public Tenant seedNbfc() {
        return tenantRepository
                .findByTenantCode("TENANT-005")
                .orElseGet(
                        () -> {
                            Tenant t =
                                    Tenant.builder()
                                            .tenantCode("TENANT-005")
                                            .tenantName("Finserv Capital NBFC")
                                            .status("ACTIVE")
                                            .currentBusinessDate(SeederDateUtil.nextWeekday())
                                            .dayStatus(DayStatus.OPEN)
                                            .country("IN")
                                            .baseCurrency("INR")
                                            .timezone("Asia/Kolkata")
                                            .regulatoryCode("RBI/2024/NBFC/005")
                                            .multiBranchEnabled(true)
                                            .eodStatus("NOT_STARTED")
                                            .effectiveFrom(java.time.LocalDate.of(2024, 10, 1))
                                            .remarks(
                                                    "NBFC — Scale-Based Regulation (SBR) Upper Layer")
                                            .build();
                            return tenantRepository.save(t);
                        });
    }

    public void seed() {
        seedDefaultTenant();
        seedSecondTenant();
        seedCooperativeBank();
        seedRuralBank();
        seedNbfc();
        log.info("  [Tenants] 5 tenants ready (Main Bank, Partner Bank, UCB, RRB, NBFC)");
    }
}
