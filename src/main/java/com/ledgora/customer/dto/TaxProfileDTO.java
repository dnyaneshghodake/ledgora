package com.ledgora.customer.dto;

import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/** DTO for CustomerTaxProfile — used in Tab 4 of Customer Master screen. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxProfileDTO {

    private Long id;
    private Long customerMasterId;

    @Pattern(
            regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$",
            message = "PAN must match format: ABCDE1234F")
    private String panNumber;

    private String tanNumber;
    private String gstNumber;

    @Pattern(regexp = "^$|^[0-9]{12}$", message = "Aadhaar must be exactly 12 digits")
    private String aadhaarNumber;

    private Boolean tdsApplicable;
    private BigDecimal tdsRate;
    private Boolean fatcaDeclaration;
    private String taxResidencyStatus; // RESIDENT, NON_RESIDENT, NRI
    private Boolean taxDeductionFlag;
    private String exemptionCode;
    private LocalDate exemptionValidTill;

    // Audit (read-only)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
