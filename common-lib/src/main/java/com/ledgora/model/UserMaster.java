package com.ledgora.model;

import com.ledgora.model.embedded.TMDbtrack;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Master Entity - POJO Model
 *
 * <p>File Name : D002001.DDF File Desc : User Master Module Type : 002/Application Level Maint.
 * Record Size : Variable
 *
 * <p>This entity represents a system user with authentication, authorization, and access control
 * features.
 */
@Entity
@Table(name = "D002001", schema = "OASYS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMaster {

    // ========== Primary Key ==========
    /** Primary Key - User Code 1 Type: String (S), Size: 8 */
    @Id
    @Column(name = "USR_CODE1", length = 8, nullable = false)
    private String usrCode1;

    // ========== Alternative Keys ==========
    /**
     * Alternative Key 1 - User Code 2 Type: Integer (I), Size: 4 Unique identifier for user code 2
     */
    @Column(name = "USR_CODE2", length = 8, unique = true)
    private Integer usrCode2;

    /**
     * Alternative Key 2 - User Code 3 Type: String (S), Size: 16 Unique identifier for user code 3
     */
    @Column(name = "USR_CODE3", length = 16, unique = true)
    private String usrCode3;

    // ========== User Basic Information ==========
    /** User Short Name Type: String (S), Size: 10 */
    @Column(name = "USR_SHORT_NAME", length = 10)
    private String usrShortName;

    /** User Full Name Type: String (S), Size: 35 */
    @Column(name = "USR_NAME", length = 35)
    private String usrName;

    /** User Email ID Type: String (S), Size: 35 */
    @Column(name = "EMAIL_ID", length = 35)
    private String emailId;

    /** User Extension Number Type: Integer (I), Size: 2 */
    @Column(name = "EXTN_NO", length = 4)
    private Integer extnNo;

    // ========== Branch & Access Control ==========
    /**
     * User Branch Code - indicates the branch in which this user can pass vouchers. Zeroes means he
     * can pass for any branch. Type: Integer (I), Size: 4
     */
    @Column(name = "USR_BR_CODE", length = 6)
    private Integer usrBrCode;

    /** Login From Other Branch allowed Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "LGN_FRM_OTH_BR", length = 1)
    private String lgnFrmOthBr;

    /** Multi-Branch Access allowed Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "MULTI_BR_ACCESS", length = 1)
    private String multiBrAccess;

    /** Customer Access Level Type: Integer (I), Size: 2 */
    @Column(name = "CUST_ACCESS_LVL", length = 4)
    private Integer custAccessLvl;

    // ========== Group & Status ==========
    /** Group Code - indexed with usrCode1 Type: Integer (I), Size: 2 */
    @Column(name = "GRP_CD", length = 4)
    private Integer grpCd;

    /** User Status Type: Integer (I), Size: 1 */
    @Column(name = "STATUS", length = 2)
    private Integer status;

    /** Is Active flag Type: Integer (I), Size: 2 */
    @Column(name = "IS_ACTIVE", length = 4)
    private Integer isActive;

    /** Custom Status Type: String (S), Size: 50 */
    @Column(name = "CUSTOM_STATUS", length = 50)
    private String customStatus;

    // ========== Screen Lock Settings ==========
    /** Screen Lock enabled Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "SCR_LOCK_YN", length = 1)
    private String scrLockYN;

    /** Screen Lock timeout in seconds Type: Integer (I), Size: 4 */
    @Column(name = "SCR_LOCK_AFTER_SECS", length = 8)
    private Integer scrLockAfterSecs;

    // ========== Auto Logout Settings ==========
    /** Auto Logout enabled Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "AUTO_LOGOUT_YN", length = 1)
    private String autoLogoutYN;

    /** Auto Logout timeout in seconds Type: Integer (I), Size: 4 */
    @Column(name = "AUTO_LOGOUT_AFTER_SECS", length = 8)
    private Integer autoLogoutAfterSecs;

    // ========== Password Management ==========
    /** Password Change Forced Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "PWD_CHG_FORCED_YN", length = 1)
    private String pwdChgForcedYN;

    /** Password Change Period in Days Type: Integer (I), Size: 4 */
    @Column(name = "PWD_CHG_PERIOD_DAYS", length = 8)
    private Integer pwdChgPeriodDays;

    /** Last Password Change Date Type: Date (D), Size: 4 */
    @Column(name = "LAST_PWD_CHG_DT")
    private LocalDate lastPwdChgDt;

    /** Next Password Change Date Type: Date (D), Size: 4 */
    @Column(name = "NEXT_PWD_CHG_DT")
    private LocalDate nextPwdChgDt;

    /** Password Negatives Mode Type: Integer (I), Size: 1 */
    @Column(name = "PWD_NEGATIVES_MOD", length = 2)
    private Integer pwdNegativesMod;

    // ========== Minimum Login Frequency Settings ==========
    /** Minimum Login Frequency Forced Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "MIN_LI_FREQ_FORCED_YN", length = 1)
    private String minLiFreqForcedYN;

    /** Minimum Login Period in Days Type: Integer (I), Size: 4 */
    @Column(name = "MIN_LI_PERIOD_DAYS", length = 8)
    private Integer minLiPeriodDays;

    // ========== Bad Login Tracking ==========
    /** Maximum Bad Logins Per Day Type: Integer (I), Size: 4 */
    @Column(name = "MAX_BAD_LI_PER_DAY", length = 8)
    private Integer maxBadLiPerDay;

    /** Maximum Bad Logins Per Instance Type: Integer (I), Size: 4 */
    @Column(name = "MAX_BAD_LI_PER_INST", length = 8)
    private Integer maxBadLiPerInst;

    /** Bad Logins Date Type: Date (D), Size: 4 */
    @Column(name = "BAD_LOGINS_DT")
    private LocalDate badLoginsDt;

    /** Number of Bad Logins Type: Integer (I), Size: 4 */
    @Column(name = "NO_OF_BAD_LOGINS", length = 8)
    private Integer noOfBadLogins;

    // ========== Login Date Tracking ==========
    /** Last System Login Date Type: Date (D), Size: 4 */
    @Column(name = "LAST_SYS_LI_DT")
    private LocalDate lastSysLiDt;

    /** Next System Login Date Type: Date (D), Size: 4 */
    @Column(name = "NEXT_SYS_LI_DT")
    private LocalDate nextSysLiDt;

    // ========== Access Profile Settings ==========
    /** Access Profile Menu Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "ACC_PROF_MNU", length = 1)
    private String accProfMnu;

    /** Access Profile Nodes Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "ACC_PROF_NODES", length = 1)
    private String accProfNodes;

    /** Access Profile System Entities Type: String (S), Size: 1 Values: Y/N */
    @Column(name = "ACC_PROF_SYS_ENT", length = 1)
    private String accProfSysEnt;

    // ========== Report & Station Information ==========
    /** Maximum Report Files Type: Integer (I), Size: 1 */
    @Column(name = "MAX_RPT_FILES", length = 2)
    private Integer maxRptFiles;

    /** Active In Station Type: Integer (I), Size: 4 */
    @Column(name = "ACTIVE_IN_STN", length = 6)
    private Integer activeInStn;

    /** Active Serial Number Indicator Type: Integer (I), Size: 1 */
    @Column(name = "ACTIVE_SR_NO_IND", length = 3)
    private Integer activeSrNoInd;

    // ========== Customer Information ==========
    /** Customer Number Type: Integer (I), Size: 4 */
    @Column(name = "CUST_NO", length = 9)
    private Integer custNo;

    // ========== Allowance Information ==========
    /** Allowance Recount Type: Integer (I), Size: 4 */
    @Column(name = "ALLO_RE_COUNT", length = 8)
    private Integer alloReCount;

    /** Allowance Unrecount Type: Integer (I), Size: 4 */
    @Column(name = "ALLO_UN_RE_COUNT", length = 8)
    private Integer alloUnReCount;

    // ========== Embedded/Related Objects ==========
    /** Database Track Information Type: Embedded Object (TMDbtrack) Not Audited */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "createdBy", column = @Column(name = "CREATED_BY")),
        @AttributeOverride(name = "createdDt", column = @Column(name = "CREATED_DT")),
        @AttributeOverride(name = "modifiedBy", column = @Column(name = "MODIFIED_BY")),
        @AttributeOverride(name = "modifiedDt", column = @Column(name = "MODIFIED_DT"))
    })
    private TMDbtrack dbtr;

    // ========== Constructor with Primary Key ==========
    public UserMaster(String usrCode1) {
        this.usrCode1 = usrCode1;
    }

    @Override
    public String toString() {
        return "UserMaster{"
                + "usrCode1='"
                + usrCode1
                + '\''
                + ", usrCode2="
                + usrCode2
                + ", usrCode3='"
                + usrCode3
                + '\''
                + ", usrName='"
                + usrName
                + '\''
                + ", usrShortName='"
                + usrShortName
                + '\''
                + ", emailId='"
                + emailId
                + '\''
                + ", usrBrCode="
                + usrBrCode
                + ", grpCd="
                + grpCd
                + ", status="
                + status
                + ", isActive="
                + isActive
                + ", lastSysLiDt="
                + lastSysLiDt
                + '}';
    }
}
