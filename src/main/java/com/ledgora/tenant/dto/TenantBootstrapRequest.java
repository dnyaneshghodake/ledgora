package com.ledgora.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the Tenant Onboarding form (Maker step).
 *
 * <p>This DTO is submitted to POST /admin/tenants/create and serialised into the ApprovalRequest's
 * requestData JSON field. The Bootstrap Service reads it back on approval to drive the controlled
 * bootstrap sequence.
 *
 * <p>No tenant ID is present — tenant creation happens AFTER approval, not before.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantBootstrapRequest {

    // ── Section 1: Tenant Information ──

    @NotBlank(message = "Tenant Code is required")
    @Size(min = 3, max = 20, message = "Tenant Code must be 3–20 characters")
    @Pattern(
            regexp = "^[A-Z0-9_-]+$",
            message = "Tenant Code must be uppercase alphanumeric with hyphens/underscores")
    private String tenantCode;

    @NotBlank(message = "Tenant Name is required")
    @Size(max = 100, message = "Tenant Name must not exceed 100 characters")
    private String tenantName;

    @NotBlank(message = "Country is required")
    @Size(max = 5)
    @Builder.Default
    private String country = "IN";

    @NotBlank(message = "Base Currency is required")
    @Size(max = 5)
    @Builder.Default
    private String baseCurrency = "INR";

    @NotBlank(message = "Timezone is required")
    @Size(max = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    /**
     * RBI-assigned regulatory code. Format: RBI/YYYY/TYPE/NNN. Validated server-side against the
     * pattern.
     */
    @NotBlank(message = "Regulatory Code is required")
    @Pattern(
            regexp = "^RBI/[0-9]{4}/[A-Z]+/[0-9]{3,}$",
            message =
                    "Regulatory Code must match format RBI/YYYY/TYPE/NNN (e.g. RBI/2024/BANK/001)")
    private String regulatoryCode;

    // ── Section 2: Initial Business Setup ──

    @NotNull(message = "Initial Business Date is required")
    private LocalDate initialBusinessDate;

    @Builder.Default private Boolean multiBranchEnabled = false;

    // ── Section 3: Governance ──

    @NotNull(message = "Effective From date is required")
    private LocalDate effectiveFrom;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
