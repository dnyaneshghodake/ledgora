package com.ledgora.loan.repository;

import com.ledgora.loan.entity.LoanProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    Optional<LoanProduct> findByProductCode(String productCode);

    List<LoanProduct> findByTenantIdAndIsActiveTrue(Long tenantId);

    List<LoanProduct> findByTenantId(Long tenantId);
}
