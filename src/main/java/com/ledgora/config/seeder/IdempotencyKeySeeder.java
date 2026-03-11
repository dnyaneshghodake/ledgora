package com.ledgora.config.seeder;

import com.ledgora.idempotency.entity.IdempotencyKey;
import com.ledgora.idempotency.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 9 — Idempotency Key seeding. Seeds sample idempotency keys for testing
 * deduplication.
 */
@Component
public class IdempotencyKeySeeder {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeySeeder.class);
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyKeySeeder(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    public void seed() {
        if (idempotencyKeyRepository.count() > 0) {
            log.info("  [Idempotency] Keys already exist — skipping");
            return;
        }

        createKey("DEP-SEED-0001:TELLER", "deposit-hash-001", "COMPLETED");
        createKey("DEP-SEED-0002:TELLER", "deposit-hash-002", "COMPLETED");
        createKey("TRF-SEED-0001:ONLINE", "transfer-hash-001", "COMPLETED");
        createKey("WDR-SEED-0001:ATM", "withdrawal-hash-001", "COMPLETED");
        createKey("TEST-IDEM-001:MOBILE", "test-hash-001", "PROCESSING");

        log.info("  [Idempotency] 5 sample idempotency keys seeded");
    }

    private void createKey(String key, String requestHash, String status) {
        IdempotencyKey ik =
                IdempotencyKey.builder().key(key).requestHash(requestHash).status(status).build();
        idempotencyKeyRepository.save(ik);
    }
}
