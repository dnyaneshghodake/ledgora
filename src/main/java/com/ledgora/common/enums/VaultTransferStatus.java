package com.ledgora.common.enums;

/** Status of a vault cash transfer (teller ↔ vault). Requires dual authorization per RBI. */
public enum VaultTransferStatus {
    /** Transfer initiated by first custodian. */
    INITIATED,
    /** Transfer authorized by second custodian (dual custody). */
    AUTHORIZED,
    /** Transfer rejected by second custodian. */
    REJECTED
}
