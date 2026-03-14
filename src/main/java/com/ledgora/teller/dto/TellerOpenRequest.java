package com.ledgora.teller.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;

/** Request DTO for opening a teller session. Includes opening cash denomination breakdown. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TellerOpenRequest {

    /** Opening cash balance. Must equal SUM(denomination entries). */
    private BigDecimal openingBalance;

    /** Denomination breakdown of opening cash. */
    private List<DenominationEntry> denominations;
}
