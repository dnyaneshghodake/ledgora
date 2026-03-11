package com.ledgora.product.repository;

import com.ledgora.common.enums.ProductStatus;
import com.ledgora.common.enums.ProductType;
import com.ledgora.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND p.productCode = :productCode")
    Optional<Product> findByTenantIdAndProductCode(
            @Param("tenantId") Long tenantId, @Param("productCode") String productCode);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId")
    List<Product> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId")
    Page<Product> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND p.status = :status")
    List<Product> findByTenantIdAndStatus(
            @Param("tenantId") Long tenantId, @Param("status") ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND p.productType = :productType AND p.status = 'ACTIVE'")
    List<Product> findActiveByTenantIdAndType(
            @Param("tenantId") Long tenantId, @Param("productType") ProductType productType);

    boolean existsByTenantIdAndProductCode(Long tenantId, String productCode);
}
