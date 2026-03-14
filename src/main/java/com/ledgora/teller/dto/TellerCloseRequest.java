package com.ledgora.teller.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;

/** Request DTO for teller closure. Teller declares physical cash for reconciliation. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TellerCloseRequest {

    /** Physical cash amount declared by teller. */
    private BigDecimal declaredAmount;

    /** Denomination breakdown of declared cash. */
    private List<DenominationEntry> denominations;
}
