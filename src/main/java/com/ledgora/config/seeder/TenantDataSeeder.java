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
                                            .currentBusinessDate(
                                                    SeederDateUtil.nextWeekday())
                                            .dayStatus(DayStatus.OPEN)
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
                                            .currentBusinessDate(
                                                    SeederDateUtil.nextWeekday())
                                            .dayStatus(DayStatus.OPEN)
                                            .build();
                            return tenantRepository.save(t);
                        });
    }

    public void seed() {
        seedDefaultTenant();
        seedSecondTenant();
        log.info("  [Tenants] Default tenant (TENANT-001) and secondary tenant (TENANT-002) ready");
    }
}
