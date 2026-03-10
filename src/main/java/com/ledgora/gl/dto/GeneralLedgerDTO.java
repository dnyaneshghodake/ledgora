package com.ledgora.gl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

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
