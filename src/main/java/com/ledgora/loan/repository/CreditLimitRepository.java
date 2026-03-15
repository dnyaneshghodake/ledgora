package com.ledgora.loan.repository;

import com.ledgora.loan.entity.CreditLimit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Credit Limit Repository — credit facility queries per RBI exposure norms. */
@Repository
public interface CreditLimitRepository extends JpaRepository<CreditLimit, Long> {

    /** All active limits for a borrower. */
    @Query(
            "SELECT cl FROM CreditLimit cl "
                    + "WHERE cl.tenant.id = :tenantId "
                    + "AND cl.borrowerId = :borrowerId "
                    + "AND cl.status = 'ACTIVE'")
    List<CreditLimit> findActiveLimitsByBorrower(
            @Param("tenantId") Long tenantId, @Param("borrowerId") String borrowerId);

    /** All active limits for a tenant. */
    @Query(
            "SELECT cl FROM CreditLimit cl "
                    + "WHERE cl.tenant.id = :tenantId AND cl.status = 'ACTIVE'")
    List<CreditLimit> findActiveByTenantId(@Param("tenantId") Long tenantId);

    /** Find by limit reference (unique). */
    Optional<CreditLimit> findByLimitReference(String limitReference);

    /** Total sanctioned for a borrower (for exposure cap checks). */
    @Query(
            "SELECT COALESCE(SUM(cl.sanctionedAmount), 0) FROM CreditLimit cl "
                    + "WHERE cl.tenant.id = :tenantId "
                    + "AND cl.borrowerId = :borrowerId "
                    + "AND cl.status = 'ACTIVE'")
    java.math.BigDecimal sumSanctionedByBorrower(
            @Param("tenantId") Long tenantId, @Param("borrowerId") String borrowerId);

    /** Total utilized for a borrower. */
    @Query(
            "SELECT COALESCE(SUM(cl.utilizedAmount), 0) FROM CreditLimit cl "
                    + "WHERE cl.tenant.id = :tenantId "
                    + "AND cl.borrowerId = :borrowerId "
                    + "AND cl.status = 'ACTIVE'")
    java.math.BigDecimal sumUtilizedByBorrower(
            @Param("tenantId") Long tenantId, @Param("borrowerId") String borrowerId);

    /** Sector-wise total sanctioned (for sector cap checks). */
    @Query(
            "SELECT COALESCE(SUM(cl.sanctionedAmount), 0) FROM CreditLimit cl "
                    + "WHERE cl.tenant.id = :tenantId "
                    + "AND cl.sector = :sector "
                    + "AND cl.status = 'ACTIVE'")
    java.math.BigDecimal sumSanctionedBySector(
            @Param("tenantId") Long tenantId, @Param("sector") String sector);
}
