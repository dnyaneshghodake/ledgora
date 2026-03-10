package com.ledgora.common.enums;

/**
 * Derived status for CBS voucher lifecycle.
 * Ground truth remains the (authFlag, postFlag, cancelFlag) triple on the Voucher entity.
 * This enum is a convenience projection for UI display and query filtering.
 *
 * Mapping:
 *   DRAFT            = authFlag=N, postFlag=N, cancelFlag=N
 *   PENDING_APPROVAL = authFlag=N, postFlag=N, cancelFlag=N  (same as DRAFT — alias for UI)
 *   APPROVED         = authFlag=Y, postFlag=N, cancelFlag=N
 *   POSTED           = authFlag=Y, postFlag=Y, cancelFlag=N
 *   REVERSED         = cancelFlag=Y
 */
public enum VoucherStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    POSTED,
    REVERSED;

    /**
     * Derive status from the ground-truth flag triple.
     */
    public static VoucherStatus fromFlags(String authFlag, String postFlag, String cancelFlag) {
        if ("Y".equals(cancelFlag)) return REVERSED;
        if ("Y".equals(postFlag))   return POSTED;
        if ("Y".equals(authFlag))   return APPROVED;
        return DRAFT;
    }
}
