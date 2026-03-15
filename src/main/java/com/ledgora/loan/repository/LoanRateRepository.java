package com.ledgora.loan.repository;

import com.ledgora.loan.entity.LoanRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Loan Rate Repository — Finacle LARATE data access.
 *
 * <p>Provides rate lookups by product, effective date, and active status.
 */
@Repository
public interface LoanRateRepository extends JpaRepository<LoanRate, Long> {

    /** Current active rate for a product. */
    @Query(
            "SELECT lr FROM LoanRate lr "
                    + "WHERE lr.loanProduct.id = :productId "
                    + "AND lr.isActive = true "
                    + "ORDER BY lr.effectiveDate DESC")
    List<LoanRate> findActiveByProductId(@Param("productId") Long productId);

    /** Rate effective on a specific date for a product. */
    @Query(
            "SELECT lr FROM LoanRate lr "
                    + "WHERE lr.loanProduct.id = :productId "
                    + "AND lr.effectiveDate <= :asOfDate "
                    + "AND (lr.endDate IS NULL OR lr.endDate >= :asOfDate) "
                    + "ORDER BY lr.effectiveDate DESC")
    List<LoanRate> findEffectiveByProductIdAndDate(
            @Param("productId") Long productId, @Param("asOfDate") LocalDate asOfDate);

    /** All rates for a product ordered by effective date (history view). */
    List<LoanRate> findByLoanProductIdOrderByEffectiveDateDesc(Long loanProductId);

    /** All rates for a tenant. */
    List<LoanRate> findByTenantIdOrderByEffectiveDateDesc(Long tenantId);

    /** Find by product and exact effective date (unique constraint). */
    Optional<LoanRate> findByLoanProductIdAndEffectiveDate(
            Long loanProductId, LocalDate effectiveDate);
}
