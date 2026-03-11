package com.ledgora.product.service;

import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.product.entity.Product;
import com.ledgora.product.entity.ProductGlMapping;
import com.ledgora.product.entity.ProductVersion;
import com.ledgora.product.repository.ProductGlMappingRepository;
import com.ledgora.product.repository.ProductVersionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * CBS-grade Product Validation Service. Enforces business rules before a Product or ProductVersion
 * can be approved/activated:
 *
 * <ul>
 *   <li>GL mappings must exist for the effective version before product can be ACTIVE
 *   <li>All GL codes referenced in mappings must exist in the GL hierarchy
 *   <li>ProductVersion rows are immutable once APPROVED
 *   <li>Effective dates must not overlap for the same product
 * </ul>
 */
@Service
public class ProductValidationService {

    private static final Logger log = LoggerFactory.getLogger(ProductValidationService.class);
    private final ProductVersionRepository versionRepository;
    private final ProductGlMappingRepository glMappingRepository;
    private final GeneralLedgerRepository glRepository;

    public ProductValidationService(
            ProductVersionRepository versionRepository,
            ProductGlMappingRepository glMappingRepository,
            GeneralLedgerRepository glRepository) {
        this.versionRepository = versionRepository;
        this.glMappingRepository = glMappingRepository;
        this.glRepository = glRepository;
    }

    /**
     * Validate that a Product can be activated. Checks: 1. At least one APPROVED version exists 2.
     * The effective version has a GL mapping 3. All GL codes in the mapping are valid
     *
     * @return list of validation errors (empty = valid)
     */
    public List<String> validateForActivation(Product product) {
        List<String> errors = new ArrayList<>();

        // 1. Must have at least one approved version
        List<ProductVersion> approvedVersions =
                versionRepository.findByProductIdAndStatus(
                        product.getId(), MakerCheckerStatus.APPROVED);
        if (approvedVersions.isEmpty()) {
            errors.add(
                    "Product has no approved versions. At least one version must be approved before activation.");
            return errors;
        }

        // 2. Check effective version has GL mapping
        Optional<ProductVersion> effectiveVersion =
                versionRepository.findEffectiveVersion(product.getId(), LocalDate.now());
        if (effectiveVersion.isEmpty()) {
            errors.add(
                    "No effective version found for current date. Check effective_from/effective_to dates.");
            return errors;
        }

        ProductVersion version = effectiveVersion.get();
        Optional<ProductGlMapping> mappingOpt =
                glMappingRepository.findByProductVersionId(version.getId());
        if (mappingOpt.isEmpty()) {
            errors.add(
                    "GL mapping missing for effective version "
                            + version.getVersionNumber()
                            + ". GL mappings must be configured before product activation.");
            return errors;
        }

        // 3. Validate all GL codes exist in the hierarchy
        ProductGlMapping mapping = mappingOpt.get();
        validateGlCode(mapping.getDrGlCode(), "DR GL", errors);
        validateGlCode(mapping.getCrGlCode(), "CR GL", errors);
        validateGlCode(mapping.getSuspenseGlCode(), "Suspense GL", errors);
        validateGlCode(mapping.getInterestAccrualGlCode(), "Interest Accrual GL", errors);
        if (mapping.getClearingGlCode() != null && !mapping.getClearingGlCode().isBlank()) {
            validateGlCode(mapping.getClearingGlCode(), "Clearing GL", errors);
        }

        if (errors.isEmpty()) {
            log.info("Product {} passed activation validation", product.getProductCode());
        } else {
            log.warn(
                    "Product {} failed activation validation: {}",
                    product.getProductCode(),
                    errors);
        }

        return errors;
    }

    /**
     * Validate that a ProductVersion can be approved. Checks: 1. Version is currently PENDING 2. GL
     * mapping exists for this version
     */
    public List<String> validateVersionForApproval(ProductVersion version) {
        List<String> errors = new ArrayList<>();

        if (version.getStatus() != MakerCheckerStatus.PENDING) {
            errors.add(
                    "Version "
                            + version.getVersionNumber()
                            + " is not pending approval. Current status: "
                            + version.getStatus());
        }

        if (!glMappingRepository.existsByProductVersionId(version.getId())) {
            errors.add(
                    "GL mapping must be configured before version "
                            + version.getVersionNumber()
                            + " can be approved.");
        }

        return errors;
    }

    /**
     * Validate that a ProductVersion is still mutable (not yet approved). Once APPROVED, version
     * rows and their GL mappings are immutable.
     */
    public void assertVersionMutable(ProductVersion version) {
        if (version.getStatus() == MakerCheckerStatus.APPROVED) {
            throw new RuntimeException(
                    "ProductVersion "
                            + version.getVersionNumber()
                            + " is APPROVED and immutable. Create a new version instead.");
        }
    }

    private void validateGlCode(String glCode, String label, List<String> errors) {
        if (glCode == null || glCode.isBlank()) {
            errors.add(label + " code is missing.");
            return;
        }
        if (glRepository.findByGlCode(glCode).isEmpty()) {
            errors.add(label + " code '" + glCode + "' does not exist in the GL hierarchy.");
        }
    }
}
