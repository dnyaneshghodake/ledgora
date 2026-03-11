package com.ledgora.product.repository;

import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.product.entity.ProductVersion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVersionRepository extends JpaRepository<ProductVersion, Long> {

    List<ProductVersion> findByProductIdOrderByVersionNumberDesc(Long productId);

    @Query(
            "SELECT pv FROM ProductVersion pv WHERE pv.product.id = :productId AND pv.status = :status ORDER BY pv.versionNumber DESC")
    List<ProductVersion> findByProductIdAndStatus(
            @Param("productId") Long productId, @Param("status") MakerCheckerStatus status);

    /** Find the currently effective version for a product on a given date. */
    @Query(
            "SELECT pv FROM ProductVersion pv WHERE pv.product.id = :productId AND pv.status = 'APPROVED' AND pv.effectiveFrom <= :date AND (pv.effectiveTo IS NULL OR pv.effectiveTo >= :date) ORDER BY pv.versionNumber DESC")
    Optional<ProductVersion> findEffectiveVersion(
            @Param("productId") Long productId, @Param("date") LocalDate date);

    @Query("SELECT MAX(pv.versionNumber) FROM ProductVersion pv WHERE pv.product.id = :productId")
    Optional<Integer> findMaxVersionNumber(@Param("productId") Long productId);
}
