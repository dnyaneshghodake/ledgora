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

    @NotNull(message = "Account type is required")
    private String accountType;

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
