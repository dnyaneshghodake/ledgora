package com.ledgora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
