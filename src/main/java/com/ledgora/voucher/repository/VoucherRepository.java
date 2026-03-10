package com.ledgora.voucher.repository;

import com.ledgora.voucher.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    @Query("SELECT v FROM Voucher v WHERE v.tenant.id = :tenantId AND v.branch.id = :branchId AND v.postingDate = :postingDate")
    List<Voucher> findByTenantIdAndBranchIdAndPostingDate(@Param("tenantId") Long tenantId,
                                                           @Param("branchId") Long branchId,
                                                           @Param("postingDate") LocalDate postingDate);

    @Query("SELECT v FROM Voucher v WHERE v.tenant.id = :tenantId AND v.postingDate = :postingDate AND v.authFlag = 'N'")
    List<Voucher> findUnauthorizedVouchers(@Param("tenantId") Long tenantId,
                                            @Param("postingDate") LocalDate postingDate);

    @Query("SELECT v FROM Voucher v WHERE v.tenant.id = :tenantId AND v.postingDate = :postingDate AND v.postFlag = 'N' AND v.authFlag = 'Y' AND v.cancelFlag = 'N'")
    List<Voucher> findAuthorizedUnpostedVouchers(@Param("tenantId") Long tenantId,
                                                   @Param("postingDate") LocalDate postingDate);

    @Query("SELECT v FROM Voucher v WHERE v.tenant.id = :tenantId AND v.postingDate = :postingDate AND v.postFlag = 'N' AND v.cancelFlag = 'N'")
    List<Voucher> findUnpostedVouchers(@Param("tenantId") Long tenantId,
                                        @Param("postingDate") LocalDate postingDate);

    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.tenant.id = :tenantId AND v.postingDate = :postingDate AND v.authFlag = 'N' AND v.cancelFlag = 'N'")
    long countUnauthorizedVouchers(@Param("tenantId") Long tenantId,
                                    @Param("postingDate") LocalDate postingDate);

    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.tenant.id = :tenantId AND v.postingDate = :postingDate AND v.postFlag = 'N' AND v.cancelFlag = 'N'")
    long countUnpostedVouchers(@Param("tenantId") Long tenantId,
                                @Param("postingDate") LocalDate postingDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id")
    Optional<Voucher> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT v FROM Voucher v WHERE v.tenant.id = :tenantId AND v.id = :id")
    Optional<Voucher> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    Optional<Voucher> findByVoucherNumber(String voucherNumber);

    @Query("SELECT v FROM Voucher v WHERE v.transaction.id = :transactionId ORDER BY v.id")
    List<Voucher> findByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * EOD check: count vouchers that are APPROVED (auth=Y) but NOT yet posted and NOT cancelled.
     */
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.tenant.id = :tenantId AND v.postingDate = :postingDate AND v.authFlag = 'Y' AND v.postFlag = 'N' AND v.cancelFlag = 'N'")
    long countApprovedUnpostedVouchers(@Param("tenantId") Long tenantId,
                                        @Param("postingDate") LocalDate postingDate);

    /**
     * EOD ledger-balance check: sum of total_debit for all POSTED vouchers on a date.
     */
    @Query("SELECT COALESCE(SUM(v.totalDebit), 0) FROM Voucher v WHERE v.tenant.id = :tenantId AND v.postingDate = :postingDate AND v.postFlag = 'Y' AND v.cancelFlag = 'N'")
    BigDecimal sumPostedDebits(@Param("tenantId") Long tenantId,
                                @Param("postingDate") LocalDate postingDate);

    /**
     * EOD ledger-balance check: sum of total_credit for all POSTED vouchers on a date.
     */
    @Query("SELECT COALESCE(SUM(v.totalCredit), 0) FROM Voucher v WHERE v.tenant.id = :tenantId AND v.postingDate = :postingDate AND v.postFlag = 'Y' AND v.cancelFlag = 'N'")
    BigDecimal sumPostedCredits(@Param("tenantId") Long tenantId,
                                 @Param("postingDate") LocalDate postingDate);

    @Query("SELECT v FROM Voucher v WHERE v.tenant.id = :tenantId AND v.account.id = :accountId AND v.postingDate = :postingDate")
    List<Voucher> findByTenantIdAndAccountIdAndPostingDate(@Param("tenantId") Long tenantId,
                                                            @Param("accountId") Long accountId,
                                                            @Param("postingDate") LocalDate postingDate);
}
