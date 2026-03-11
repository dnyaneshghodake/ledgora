package com.ledgora.product.repository;

import com.ledgora.product.entity.ProductGlMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductGlMappingRepository extends JpaRepository<ProductGlMapping, Long> {

    Optional<ProductGlMapping> findByProductVersionId(Long productVersionId);

    boolean existsByProductVersionId(Long productVersionId);
}
