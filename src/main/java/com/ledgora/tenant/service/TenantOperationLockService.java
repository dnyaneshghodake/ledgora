package com.ledgora.tenant.service;

import com.ledgora.tenant.entity.TenantOperationLock;
import com.ledgora.tenant.repository.TenantOperationLockRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS-grade Tenant Operation Lock Service. Ensures mutual exclusion of critical batch operations
 * (EOD, Settlement, Reconciliation) per tenant.
 *
 * <p>Usage pattern:
 *
 * <pre>
 *   tenantOperationLockService.acquireLock(tenantId, "EOD", "admin");
 *   try {
 *       // ... run EOD phases ...
 *   } finally {
 *       tenantOperationLockService.releaseLock(tenantId);
 *   }
 * </pre>
 *
 * <p>The lock is implemented via SELECT ... FOR UPDATE on the lock row. If another process is
 * already running EOD or Settlement for the same tenant, the acquire call blocks until the first
 * process commits/rollbacks.
 *
 * <p>Additionally, the lock row tracks which operation is active (for dashboard visibility and
 * stale lock detection).
 */
@Service
public class TenantOperationLockService {

    private static final Logger log = LoggerFactory.getLogger(TenantOperationLockService.class);
    private final TenantOperationLockRepository lockRepository;

    public TenantOperationLockService(TenantOperationLockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    /**
     * Acquire the tenant operation lock. Blocks if another operation is running.
     *
     * @param tenantId the tenant to lock
     * @param operation the operation name (EOD, SETTLEMENT, RECONCILIATION)
     * @param username the user/process acquiring the lock
     * @throws RuntimeException if the lock row indicates a concurrent operation
     */
    @Transactional
    public TenantOperationLock acquireLock(Long tenantId, String operation, String username) {
        // Get or create the lock row
        TenantOperationLock lock =
                lockRepository
                        .findByTenantId(tenantId)
                        .orElseGet(
                                () -> {
                                    TenantOperationLock newLock =
                                            TenantOperationLock.builder()
                                                    .tenantId(tenantId)
                                                    .build();
                                    return lockRepository.save(newLock);
                                });

        // Acquire pessimistic write lock (SELECT ... FOR UPDATE)
        lock =
                lockRepository
                        .findByTenantIdWithLock(tenantId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Failed to acquire operation lock for tenant "
                                                        + tenantId));

        // Check if another operation is already active (stale lock detection)
        if (lock.getLockedByOperation() != null
                && lock.getLockedAt() != null
                && lock.getReleasedAt() == null) {
            // Check if lock is stale (held for more than 30 minutes)
            LocalDateTime staleCutoff = LocalDateTime.now().minusMinutes(30);
            if (lock.getLockedAt().isAfter(staleCutoff)) {
                throw new RuntimeException(
                        "Tenant "
                                + tenantId
                                + " is already locked by operation '"
                                + lock.getLockedByOperation()
                                + "' (acquired by "
                                + lock.getLockedByUser()
                                + " at "
                                + lock.getLockedAt()
                                + "). Cannot run "
                                + operation
                                + " concurrently.");
            } else {
                log.warn(
                        "Stale lock detected for tenant {} operation {} (locked at {}). Overriding.",
                        tenantId,
                        lock.getLockedByOperation(),
                        lock.getLockedAt());
            }
        }

        // Acquire the lock
        lock.setLockedByOperation(operation);
        lock.setLockedByUser(username);
        lock.setLockedAt(LocalDateTime.now());
        lock.setReleasedAt(null);
        lockRepository.save(lock);

        log.info(
                "Operation lock acquired: tenant={} operation={} user={}",
                tenantId,
                operation,
                username);
        return lock;
    }

    /**
     * Release the tenant operation lock.
     *
     * @param tenantId the tenant to unlock
     */
    @Transactional
    public void releaseLock(Long tenantId) {
        lockRepository
                .findByTenantId(tenantId)
                .ifPresent(
                        lock -> {
                            String operation = lock.getLockedByOperation();
                            lock.setLockedByOperation(null);
                            lock.setLockedByUser(null);
                            lock.setReleasedAt(LocalDateTime.now());
                            lockRepository.save(lock);
                            log.info(
                                    "Operation lock released: tenant={} operation={}",
                                    tenantId,
                                    operation);
                        });
    }

    /** Check if a tenant is currently locked by any operation. */
    public boolean isLocked(Long tenantId) {
        return lockRepository
                .findByTenantId(tenantId)
                .map(lock -> lock.getLockedByOperation() != null && lock.getReleasedAt() == null)
                .orElse(false);
    }

    /** Get the current lock status for a tenant (for dashboard display). */
    public String getLockStatus(Long tenantId) {
        return lockRepository
                .findByTenantId(tenantId)
                .filter(lock -> lock.getLockedByOperation() != null && lock.getReleasedAt() == null)
                .map(
                        lock ->
                                lock.getLockedByOperation()
                                        + " by "
                                        + lock.getLockedByUser()
                                        + " since "
                                        + lock.getLockedAt())
                .orElse("UNLOCKED");
    }
}
