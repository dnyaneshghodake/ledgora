package com.ledgora.transaction.dto.view;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Account impact summary for Transaction 360° View. Balances derived from ledger, not cache. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountImpactDTO {
    private Long accountId;
    private String accountNumber;
    private String accountName;
    private String branchCode;
    private String drCr;
    private BigDecimal amount;
    private BigDecimal preBalance;
    private BigDecimal postBalance;
    private String freezeLevel;
    private BigDecimal lienAmount;
}
