package com.ledgora.common.enums;

public enum RoleName {
    ROLE_ADMIN, ROLE_MANAGER, ROLE_TELLER, ROLE_CUSTOMER,
    ROLE_MAKER, ROLE_CHECKER, ROLE_BRANCH_MANAGER, ROLE_TENANT_ADMIN, ROLE_SUPER_ADMIN,
    ROLE_OPERATIONS, ROLE_AUDITOR, ROLE_ATM_SYSTEM,
    /** System pseudo-role for SYSTEM_AUTO user. Cannot login via UI. Used as checker in STP flows. */
    ROLE_SYSTEM
}
