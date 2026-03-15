package com.ledgora.deposit.repository;

import com.ledgora.deposit.entity.DepositProduct;
import com.ledgora.deposit.enums.DepositType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositProductRepository extends JpaRepository<DepositProduct, Long> {

    Optional<DepositProduct> findByProductCode(String productCode);

    List<DepositProduct> findByTenantIdAndIsActiveTrue(Long tenantId);

    List<DepositProduct> findByTenantIdAndDepositType(Long tenantId, DepositType type);
}
