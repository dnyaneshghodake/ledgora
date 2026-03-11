package com.ledgora.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {
    private Long id;
    private String accountNumber;

    @NotBlank(message = "Account name is required")
    private String accountName;

    /**
     * @deprecated Use productId instead. accountType will be derived from the Product. Retained for
     *     backward compatibility with legacy account creation flows.
     */
    @Deprecated
    @NotNull(message = "Account type is required")
    private String accountType;

    /**
     * CBS Product engine: product under which this account is opened. When set, account type, GL
     * codes, and interest rules are derived from the product's effective version. Takes precedence
     * over accountType if both are provided.
     */
    private Long productId;

    private String status;
    private BigDecimal balance;
    private String currency;
    private String branchCode;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String customerEmail;
    private String customerPhone;
    private String glAccountCode;

    // Additional fields for UI alignment
    private Long customerId;
    private Long parentAccountId;
    private BigDecimal interestRate;
    private BigDecimal overdraftLimit;
    private String freezeLevel;
    private String freezeReason;
    private String approvalStatus;
    private BigDecimal availableBalance;
    private BigDecimal totalLien;
    private String createdAt;
}
