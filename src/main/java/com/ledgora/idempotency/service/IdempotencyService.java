package com.ledgora.idempotency.service;

import com.ledgora.idempotency.entity.IdempotencyKey;
import com.ledgora.idempotency.repository.IdempotencyKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * PART 4: Idempotency service for financial operations. Checks and stores idempotency keys to
 * prevent duplicate processing.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    /**
     * Check if an idempotency key already exists and is completed.
     *
     * @return Optional with the existing key if found and completed
     */
    public Optional<IdempotencyKey> checkExisting(String key) {
        return idempotencyKeyRepository
                .findByKey(key)
                .filter(k -> "COMPLETED".equals(k.getStatus()));
    }

    /**
     * Register a new idempotency key before processing.
     *
     * @return the created IdempotencyKey, or empty if already exists
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyKey> registerKey(String key, String requestData) {
        if (idempotencyKeyRepository.existsByKey(key)) {
            log.warn("Idempotency key already exists: {}", key);
            return Optional.empty();
        }

        IdempotencyKey idempotencyKey =
                IdempotencyKey.builder()
                        .key(key)
                        .requestHash(hashString(requestData))
                        .status("PROCESSING")
                        .build();
        return Optional.of(idempotencyKeyRepository.save(idempotencyKey));
    }

    /** Mark an idempotency key as completed with the response. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeKey(String key, String responseData) {
        idempotencyKeyRepository
                .findByKey(key)
                .ifPresent(
                        k -> {
                            k.setStatus("COMPLETED");
                            k.setResponseHash(hashString(responseData));
                            k.setResponseBody(responseData);
                            idempotencyKeyRepository.save(k);
                            log.debug("Idempotency key completed: {}", key);
                        });
    }

    /** Mark an idempotency key as failed. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failKey(String key) {
        idempotencyKeyRepository
                .findByKey(key)
                .ifPresent(
                        k -> {
                            k.setStatus("FAILED");
                            idempotencyKeyRepository.save(k);
                        });
    }

    private String hashString(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
