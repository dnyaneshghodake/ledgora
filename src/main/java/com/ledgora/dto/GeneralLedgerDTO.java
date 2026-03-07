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
public class GeneralLedgerDTO {

    private Long id;

    @NotBlank(message = "GL Code is required")
    private String glCode;

    @NotBlank(message = "GL Name is required")
    private String glName;

    private String description;

    @NotNull(message = "Account type is required")
    private String accountType;

    private Long parentId;

    private String parentGlCode;

    private Integer level;

    private Boolean isActive;

    private BigDecimal balance;

    private String normalBalance;
}
