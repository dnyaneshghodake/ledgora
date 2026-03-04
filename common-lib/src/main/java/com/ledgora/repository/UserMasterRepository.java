package com.ledgora.repository;

import com.ledgora.model.UserMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository Interface for UserMaster Entity
 *
 * <p>Provides CRUD operations and custom query methods for UserMaster. Supports finding users by
 * various keys and criteria.
 */
@Repository
public interface UserMasterRepository extends JpaRepository<UserMaster, String> {

    // ========== Find by Alternative Keys ==========

    /** Find user by Alternative Key 1 (usrCode2) */
    Optional<UserMaster> findByUsrCode2(Integer usrCode2);

    /** Find user by Alternative Key 2 (usrCode3) */
    Optional<UserMaster> findByUsrCode3(String usrCode3);

    // ========== Find by Branch & Group ==========

    /** Find all users by Branch Code */
    List<UserMaster> findByUsrBrCode(Integer usrBrCode);

    /** Find all users by Group Code */
    List<UserMaster> findByGrpCd(Integer grpCd);

    /** Find users by Branch Code and Group Code (composite key) */
    List<UserMaster> findByUsrBrCodeAndGrpCd(Integer usrBrCode, Integer grpCd);

    /** Find users by Branch Code and User Code 1 */
    List<UserMaster> findByUsrBrCodeAndUsrCode1(Integer usrBrCode, String usrCode1);

    // ========== Find by Status ==========

    /** Find all active users */
    List<UserMaster> findByIsActive(Integer isActive);

    /** Find all users by status */
    List<UserMaster> findByStatus(Integer status);

    /** Find users by status and isActive */
    List<UserMaster> findByStatusAndIsActive(Integer status, Integer isActive);

    // ========== Find by Name ==========

    /** Find users by user name (case-insensitive) */
    List<UserMaster> findByUsrNameContainingIgnoreCase(String usrName);

    /** Find users by short name (case-insensitive) */
    List<UserMaster> findByUsrShortNameContainingIgnoreCase(String usrShortName);

    /** Find users by email ID (case-insensitive) */
    Optional<UserMaster> findByEmailIdIgnoreCase(String emailId);

    // ========== Find by Access Control ==========

    /** Find all users with multi-branch access */
    List<UserMaster> findByMultiBrAccess(String multiBrAccess);

    /** Find all users who can login from other branches */
    List<UserMaster> findByLgnFrmOthBr(String lgnFrmOthBr);

    /** Find all users with specific access profile menu setting */
    List<UserMaster> findByAccProfMnu(String accProfMnu);

    // ========== Custom Query Methods ==========

    /** Find all active users in a specific branch */
    @Query("SELECT u FROM UserMaster u WHERE u.usrBrCode = :brCode AND u.isActive = :isActive")
    List<UserMaster> findActiveUsersByBranch(
            @Param("brCode") Integer brCode, @Param("isActive") Integer isActive);

    /** Find all users with screen lock enabled */
    @Query("SELECT u FROM UserMaster u WHERE u.scrLockYN = 'Y'")
    List<UserMaster> findUsersWithScreenLockEnabled();

    /** Find all users with auto logout enabled */
    @Query("SELECT u FROM UserMaster u WHERE u.autoLogoutYN = 'Y'")
    List<UserMaster> findUsersWithAutoLogoutEnabled();

    /** Find all users with forced password change */
    @Query("SELECT u FROM UserMaster u WHERE u.pwdChgForcedYN = 'Y'")
    List<UserMaster> findUsersWithForcedPasswordChange();

    /** Find all users with minimum login frequency enforced */
    @Query("SELECT u FROM UserMaster u WHERE u.minLiFreqForcedYN = 'Y'")
    List<UserMaster> findUsersWithMinLoginFrequencyEnforced();

    /** Find users by customer number */
    List<UserMaster> findByCustNo(Integer custNo);

    /** Find all users by customer access level */
    List<UserMaster> findByCustAccessLvl(Integer custAccessLvl);

    /** Count active users in a branch */
    @Query(
            "SELECT COUNT(u) FROM UserMaster u WHERE u.usrBrCode = :brCode AND u.isActive = :isActive")
    long countActiveUsersByBranch(
            @Param("brCode") Integer brCode, @Param("isActive") Integer isActive);

    /** Find users with bad login attempts */
    @Query("SELECT u FROM UserMaster u WHERE u.noOfBadLogins > 0 ORDER BY u.noOfBadLogins DESC")
    List<UserMaster> findUsersWithBadLoginAttempts();

    /** Search users by multiple criteria */
    @Query(
            "SELECT u FROM UserMaster u WHERE "
                    + "(:usrName IS NULL OR u.usrName LIKE CONCAT('%', :usrName, '%')) AND "
                    + "(:brCode IS NULL OR u.usrBrCode = :brCode) AND "
                    + "(:grpCd IS NULL OR u.grpCd = :grpCd) AND "
                    + "(:isActive IS NULL OR u.isActive = :isActive)")
    List<UserMaster> searchUsers(
            @Param("usrName") String usrName,
            @Param("brCode") Integer brCode,
            @Param("grpCd") Integer grpCd,
            @Param("isActive") Integer isActive);
}
