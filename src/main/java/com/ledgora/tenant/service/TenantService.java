package com.ledgora.tenant.service;

import com.ledgora.common.enums.DayStatus;
import com.ledgora.common.exception.BusinessDayClosedException;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for tenant management including business date and day status control. */
@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant getTenantById(Long id) {
        return tenantRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + id));
    }

    public Optional<Tenant> findByTenantCode(String code) {
        return tenantRepository.findByTenantCode(code);
    }

    public List<Tenant> getAllActiveTenants() {
        return tenantRepository.findByStatus("ACTIVE");
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    /**
     * Validate that the tenant's business day is OPEN before allowing transactions. Throws
     * BusinessDayClosedException if not OPEN.
     */
    public void validateBusinessDayOpen(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        if (tenant.getDayStatus() != DayStatus.OPEN) {
            throw new BusinessDayClosedException(tenantId, tenant.getDayStatus().name());
        }
    }

    /** Get the current business date for a tenant. */
    public LocalDate getCurrentBusinessDate(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        return tenant.getCurrentBusinessDate();
    }

    /** Start day closing for a tenant - blocks new transactions. */
    @Transactional
    public void startDayClosing(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        if (tenant.getDayStatus() != DayStatus.OPEN) {
            throw new RuntimeException(
                    "Cannot start day closing for tenant "
                            + tenantId
                            + ". Current status: "
                            + tenant.getDayStatus());
        }
        tenant.setDayStatus(DayStatus.DAY_CLOSING);
        tenantRepository.save(tenant);
        log.info(
                "Tenant {} day status set to DAY_CLOSING for business date {}",
                tenantId,
                tenant.getCurrentBusinessDate());
    }

    /**
     * Close the day and advance to next business date for a tenant. Sets status to CLOSED (not
     * OPEN). Requires explicit Day Begin to open next day.
     */
    @Transactional
    public void closeDayAndAdvance(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        if (tenant.getDayStatus() != DayStatus.DAY_CLOSING) {
            throw new RuntimeException(
                    "Cannot close day for tenant "
                            + tenantId
                            + ". Must be in DAY_CLOSING status. Current: "
                            + tenant.getDayStatus());
        }
        LocalDate oldDate = tenant.getCurrentBusinessDate();
        tenant.setDayStatus(DayStatus.CLOSED);
        tenant.setCurrentBusinessDate(oldDate.plusDays(1));
        tenantRepository.save(tenant);
        log.info(
                "Tenant {} day CLOSED. Business date advanced from {} to {} (requires Day Begin to open)",
                tenantId,
                oldDate,
                tenant.getCurrentBusinessDate());
    }

    /**
     * Open the business day (Day Begin ceremony). Pre-checks must pass before this is called. Only
     * allowed when current status is CLOSED.
     */
    @Transactional
    public void openDay(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        if (tenant.getDayStatus() != DayStatus.CLOSED) {
            throw new RuntimeException(
                    "Cannot open day for tenant "
                            + tenantId
                            + ". Must be in CLOSED status. Current: "
                            + tenant.getDayStatus());
        }
        tenant.setDayStatus(DayStatus.OPEN);
        tenantRepository.save(tenant);
        log.info(
                "Tenant {} Day Begin: business date {} is now OPEN",
                tenantId,
                tenant.getCurrentBusinessDate());
    }

    @Transactional
    public Tenant createTenant(String tenantCode, String tenantName, LocalDate businessDate) {
        if (tenantRepository.existsByTenantCode(tenantCode)) {
            throw new RuntimeException("Tenant code already exists: " + tenantCode);
        }
        Tenant tenant =
                Tenant.builder()
                        .tenantCode(tenantCode)
                        .tenantName(tenantName)
                        .status("ACTIVE")
                        .currentBusinessDate(businessDate)
                        .dayStatus(DayStatus.OPEN)
                        .build();
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created: {} - {}", saved.getTenantCode(), saved.getTenantName());
        return saved;
    }
}
