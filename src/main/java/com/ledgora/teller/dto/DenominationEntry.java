package com.ledgora.teller.dto;

import java.math.BigDecimal;
import lombok.*;

/**
 * Single denomination line in a cash transaction request. Represents count of notes for one
 * denomination value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DenominationEntry {

    /** Face value (e.g., 2000, 500, 100). */
    private BigDecimal denominationValue;

    /** Number of notes of this denomination. */
    private Integer count;
}
