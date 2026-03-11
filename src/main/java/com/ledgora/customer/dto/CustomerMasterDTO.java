package com.ledgora.customer.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * CBS-grade Customer Master DTO — maps to all 7 Finacle-style tabs.
 *
 * <p>Tab 1: General Info Tab 2: Contact Info Tab 3: KYC & Identity Tab 4: Tax Profile Tab 5: Freeze
 * Control Tab 6: Relationships Tab 7: Audit & Approval
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerMasterDTO {

    // ── Header (read-only after creation) ──
    private Long id;
    private String customerNumber;
    private Long tenantId;
    private Long homeBranchId;
    private String homeBranchName;

    // ── Tab 1: General Info ──
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be 1-50 characters")
    @Pattern(
            regexp = "^[A-Za-z .]+$",
            message = "First name must contain only alphabets, spaces, and dots")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be 1-50 characters")
    @Pattern(
            regexp = "^[A-Za-z .]+$",
            message = "Last name must contain only alphabets, spaces, and dots")
    private String lastName;

    private String fullName; // auto-generated: firstName + " " + lastName

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    private String gender;

    @NotBlank(message = "Customer type is required")
    private String customerType; // INDIVIDUAL or CORPORATE

    // ── Tab 2: Contact Info ──
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be exactly 10 digits")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must not exceed 50 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State must not exceed 50 characters")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    // ── Tab 3: KYC & Identity ──
    @NotBlank(message = "National ID is required")
    @Size(max = 50, message = "National ID must not exceed 50 characters")
    private String nationalId;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN must match format: ABCDE1234F")
    private String panNumber;

    @Pattern(regexp = "^$|^[0-9]{12}$", message = "Aadhaar must be exactly 12 digits")
    private String aadhaarNumber;

    @NotBlank(message = "KYC status is required")
    private String kycStatus; // PENDING, VERIFIED, REJECTED, EXPIRED

    // ── Tab 4: Tax Profile ──
    private String tanNumber;
    private String gstNumber;
    private Boolean tdsApplicable;
    private BigDecimal tdsRate;
    private Boolean fatcaDeclaration;
    private String taxResidencyStatus; // RESIDENT, NON_RESIDENT, NRI
    private Boolean taxDeductionFlag;
    private String exemptionCode;
    private LocalDate exemptionValidTill;

    // ── Tab 5: Freeze Control ──
    private String freezeLevel; // NONE, DEBIT_ONLY, CREDIT_ONLY, FULL
    private String freezeReason;
    private String freezeAppliedByUsername;
    private LocalDateTime freezeAppliedAt;
    private String freezeReleasedByUsername;
    private LocalDateTime freezeReleasedAt;

    // ── Tab 6: Relationships (nested list) ──
    private List<RelationshipDTO> relationships;

    // ── Tab 7: Audit & Approval (read-only) ──
    private String makerCheckerStatus; // PENDING, APPROVED, REJECTED
    private String makerUsername;
    private LocalDateTime makerTimestamp;
    private String checkerUsername;
    private LocalDateTime checkerTimestamp;
    private String status; // ACTIVE, INACTIVE, SUSPENDED, CLOSED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
