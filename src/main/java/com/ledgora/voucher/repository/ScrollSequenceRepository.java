package com.ledgora.voucher.repository;

import com.ledgora.voucher.entity.ScrollSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ScrollSequenceRepository extends JpaRepository<ScrollSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScrollSequence s WHERE s.tenantId = :tenantId AND s.branchId = :branchId AND s.postingDate = :postingDate")
    Optional<ScrollSequence> findByTenantIdAndBranchIdAndPostingDateWithLock(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("postingDate") LocalDate postingDate);
}
