package com.ledgora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * User Master DTO - For API Request/Response
 * Used for transferring user data between layers.
 * Contains all essential user information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMasterDTO {

    // ========== Keys ==========
    private String usrCode1;
    private Integer usrCode2;
    private String usrCode3;

    // ========== Basic Information ==========
    private String usrShortName;
    private String usrName;
    private String emailId;
    private Integer extnNo;

    // ========== Branch & Access ==========
    private Integer usrBrCode;
    private String lgnFrmOthBr;
    private String multiBrAccess;
    private Integer custAccessLvl;

    // ========== Group & Status ==========
    private Integer grpCd;
    private Integer status;
    private Integer isActive;
    private String customStatus;

    // ========== Security Settings ==========
    private String scrLockYN;
    private Integer scrLockAfterSecs;
    private String autoLogoutYN;
    private Integer autoLogoutAfterSecs;

    // ========== Password Management ==========
    private String pwdChgForcedYN;
    private Integer pwdChgPeriodDays;
    private LocalDate lastPwdChgDt;
    private LocalDate nextPwdChgDt;
    private Integer pwdNegativesMod;

    // ========== Login Frequency ==========
    private String minLiFreqForcedYN;
    private Integer minLiPeriodDays;

    // ========== Bad Login Tracking ==========
    private Integer maxBadLiPerDay;
    private Integer maxBadLiPerInst;
    private LocalDate badLoginsDt;
    private Integer noOfBadLogins;

    // ========== Login Date Tracking ==========
    private LocalDate lastSysLiDt;
    private LocalDate nextSysLiDt;

    // ========== Access Profiles ==========
    private String accProfMnu;
    private String accProfNodes;
    private String accProfSysEnt;

    // ========== Report & Station ==========
    private Integer maxRptFiles;
    private Integer activeInStn;
    private Integer activeSrNoInd;

    // ========== Customer Information ==========
    private Integer custNo;

    // ========== Allowance Information ==========
    private Integer alloReCount;
    private Integer alloUnReCount;

    @Override
    public String toString() {
        return "UserMasterDTO{" +
                "usrCode1='" + usrCode1 + '\'' +
                ", usrCode2=" + usrCode2 +
                ", usrName='" + usrName + '\'' +
                ", usrBrCode=" + usrBrCode +
                ", grpCd=" + grpCd +
                ", isActive=" + isActive +
                '}';
    }
}

