package com.ledgora.transaction.service;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.clearing.repository.InterBranchTransferRepository;
import com.ledgora.common.exception.TransactionNotFoundException;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.lien.repository.AccountLienRepository;
import com.ledgora.suspense.entity.SuspenseCase;
import com.ledgora.suspense.repository.SuspenseCaseRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.transaction.dto.view.AccountImpactDTO;
import com.ledgora.transaction.dto.view.AuditTimelineDTO;
import com.ledgora.transaction.dto.view.IbtDetailDTO;
import com.ledgora.transaction.dto.view.LedgerEntryDTO;
import com.ledgora.transaction.dto.view.SuspenseDetailDTO;
import com.ledgora.transaction.dto.view.TransactionViewDTO;
import com.ledgora.transaction.dto.view.VoucherDetailDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.repository.TransactionRepository;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS-Grade Transaction 360° View Service.
 *
 * <p>Aggregates data from multiple repositories into a unified TransactionViewDTO. Performance
 * constraint: maximum 6 SELECT queries. All queries use JOIN FETCH or EntityGraph to prevent N+1.
 *
 * <p>Query budget: 1. Transaction with full graph (header + accounts + maker/checker) 2. Vouchers
 * with graph (branch, account, GL, ledger entry, maker, checker) 3. Ledger entries by transaction
 * 4. IBT by reference transaction (conditional — only if cross-branch) 5. Suspense cases by
 * transaction (conditional — only if PARKED) 6. Audit logs by entity
 */
@Service
public class TransactionViewService {

    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final InterBranchTransferRepository interBranchTransferRepository;
    private final SuspenseCaseRepository suspenseCaseRepository;
    private final AuditLogRepository auditLogRepository;
    private final AccountLienRepository accountLienRepository;

    public TransactionViewService(
            TransactionRepository transactionRepository,
            VoucherRepository voucherRepository,
            LedgerEntryRepository ledgerEntryRepository,
            InterBranchTransferRepository interBranchTransferRepository,
            SuspenseCaseRepository suspenseCaseRepository,
            AuditLogRepository auditLogRepository,
            AccountLienRepository accountLienRepository) {
        this.transactionRepository = transactionRepository;
        this.voucherRepository = voucherRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.interBranchTransferRepository = interBranchTransferRepository;
        this.suspenseCaseRepository = suspenseCaseRepository;
        this.auditLogRepository = auditLogRepository;
        this.accountLienRepository = accountLienRepository;
    }

    /**
     * Build the full Transaction 360° View DTO.
     *
     * @param transactionId the transaction primary key
     * @param checkerView true if this is an authorization screen (checker context)
     * @return fully populated TransactionViewDTO
     * @throws TransactionNotFoundException if transaction does not exist
     */
    @Transactional(readOnly = true)
    public TransactionViewDTO buildTransactionView(Long transactionId, boolean checkerView) {

        // Query 1: Transaction with full association graph
        Transaction txn =
                transactionRepository
                        .findByIdWithFullGraph(transactionId)
                        .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // Tenant isolation enforcement
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null
                && txn.getTenant() != null
                && !tenantId.equals(txn.getTenant().getId())) {
            throw new TransactionNotFoundException(transactionId);
        }

        // Query 2: Vouchers with full graph (branch, account, GL, ledger entry, maker, checker)
        List<Voucher> vouchers = voucherRepository.findByTransactionIdWithGraph(transactionId);

        // Query 3: Ledger entries by transaction
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findByTransactionId(transactionId);

        // Query 4 (conditional): IBT by reference transaction
        Optional<InterBranchTransfer> ibtOpt = Optional.empty();
        boolean isCrossBranch = isInterBranchTransaction(txn);
        if (isCrossBranch && tenantId != null) {
            ibtOpt =
                    interBranchTransferRepository.findByReferenceTransactionIdAndTenantId(
                            transactionId, tenantId);
        }

        // Query 5 (conditional): Suspense cases by transaction
        List<SuspenseCase> suspenseCases = List.of();
        boolean isParked = txn.getStatus() != null && txn.getStatus().name().equals("PARKED");
        if (isParked) {
            suspenseCases = suspenseCaseRepository.findByTransactionIdWithGraph(transactionId);
        }

        // Query 6: Audit logs for this transaction entity
        List<AuditLog> auditLogs =
                auditLogRepository.findByEntityAndEntityIdOrderByTimestampDesc(
                        "Transaction", transactionId);

        // Build the DTO
        return assembleDto(
                txn, vouchers, ledgerEntries, ibtOpt, suspenseCases, auditLogs, checkerView);
    }

    private TransactionViewDTO assembleDto(
            Transaction txn,
            List<Voucher> vouchers,
            List<LedgerEntry> ledgerEntries,
            Optional<InterBranchTransfer> ibtOpt,
            List<SuspenseCase> suspenseCases,
            List<AuditLog> auditLogs,
            boolean checkerView) {

        TransactionViewDTO dto = new TransactionViewDTO();

        // Section 1: Transaction Header
        dto.setTransactionId(txn.getId());
        dto.setTransactionRef(txn.getTransactionRef());
        dto.setTransactionType(
                txn.getTransactionType() != null ? txn.getTransactionType().name() : null);
        dto.setChannel(txn.getChannel() != null ? txn.getChannel().name() : null);
        dto.setBusinessDate(txn.getBusinessDate());
        dto.setValueDate(txn.getValueDate());
        dto.setStatus(txn.getStatus() != null ? txn.getStatus().name() : null);
        dto.setMakerUsername(
                txn.getMaker() != null
                        ? txn.getMaker().getUsername()
                        : (txn.getPerformedBy() != null
                                ? txn.getPerformedBy().getUsername()
                                : null));
        dto.setCheckerUsername(txn.getChecker() != null ? txn.getChecker().getUsername() : null);
        dto.setMakerTimestamp(txn.getMakerTimestamp());
        dto.setCheckerTimestamp(txn.getCheckerTimestamp());
        dto.setCheckerRemarks(txn.getCheckerRemarks());
        dto.setAmount(txn.getAmount());
        dto.setCurrency(txn.getCurrency());
        dto.setDescription(txn.getDescription());
        dto.setNarration(txn.getNarration());
        dto.setCreatedAt(txn.getCreatedAt());
        dto.setUpdatedAt(txn.getUpdatedAt());

        // Branch code from source account
        if (txn.getSourceAccount() != null) {
            dto.setBranchCode(txn.getSourceAccount().getBranchCode());
        } else if (txn.getDestinationAccount() != null) {
            dto.setBranchCode(txn.getDestinationAccount().getBranchCode());
        }

        // Tenant code
        if (txn.getTenant() != null) {
            dto.setTenantCode(txn.getTenant().getTenantCode());
        }

        // Reversal reference
        if (txn.getReversalOf() != null) {
            dto.setReversalOfTransactionId(txn.getReversalOf().getId());
            dto.setReversalOfTransactionRef(txn.getReversalOf().getTransactionRef());
        }

        // Section 2: Account Impact Summary (derived from vouchers + ledger)
        dto.setAccountImpacts(buildAccountImpacts(vouchers, ledgerEntries));

        // Section 3: Voucher Details
        dto.setVouchers(buildVoucherDetails(vouchers));

        // Section 4: Ledger Entries
        dto.setLedgerEntries(buildLedgerEntryDtos(ledgerEntries));

        // Section 5: IBT Panel
        dto.setIbtTransaction(ibtOpt.isPresent());
        ibtOpt.ifPresent(ibt -> dto.setIbtDetail(buildIbtDetail(ibt, vouchers)));

        // Section 6: Suspense Panel
        dto.setSuspenseTransaction(!suspenseCases.isEmpty());
        dto.setSuspenseCases(
                suspenseCases.stream().map(this::buildSuspenseDetail).collect(Collectors.toList()));

        // Section 7: Audit Trail
        dto.setAuditTrail(
                auditLogs.stream().map(this::buildAuditTimeline).collect(Collectors.toList()));

        // Authorization context
        dto.setPendingApproval(
                txn.getStatus() != null && txn.getStatus().name().equals("PENDING_APPROVAL"));
        dto.setCheckerView(checkerView);

        return dto;
    }

    /**
     * Build account impact summary by aggregating voucher and ledger data. Groups by account and
     * computes pre/post balances from the immutable ledger.
     */
    private List<AccountImpactDTO> buildAccountImpacts(
            List<Voucher> vouchers, List<LedgerEntry> ledgerEntries) {

        // Use a linked map to maintain order
        Map<Long, AccountImpactDTO> impactMap = new LinkedHashMap<>();

        for (Voucher v : vouchers) {
            if (v.getAccount() == null) {
                continue;
            }
            Long accountId = v.getAccount().getId();

            if (!impactMap.containsKey(accountId)) {
                BigDecimal lienAmount = BigDecimal.ZERO;
                try {
                    lienAmount = accountLienRepository.sumActiveLienAmountByAccountId(accountId);
                    if (lienAmount == null) {
                        lienAmount = BigDecimal.ZERO;
                    }
                } catch (Exception ignored) {
                    // Lien lookup is best-effort
                }

                AccountImpactDTO impact =
                        AccountImpactDTO.builder()
                                .accountId(accountId)
                                .accountNumber(v.getAccount().getAccountNumber())
                                .accountName(v.getAccount().getAccountName())
                                .branchCode(v.getAccount().getBranchCode())
                                .drCr(v.getDrCr() != null ? v.getDrCr().name() : null)
                                .amount(v.getTransactionAmount())
                                .freezeLevel(
                                        v.getAccount().getFreezeLevel() != null
                                                ? v.getAccount().getFreezeLevel().name()
                                                : "NONE")
                                .lienAmount(lienAmount)
                                .build();

                impactMap.put(accountId, impact);
            }
        }

        // Derive pre/post balances from ledger entries (immutable source of truth)
        for (LedgerEntry entry : ledgerEntries) {
            if (entry.getAccount() == null) {
                continue;
            }
            Long accountId = entry.getAccount().getId();
            AccountImpactDTO impact = impactMap.get(accountId);
            if (impact != null) {
                // postBalance = balanceAfter from ledger entry
                impact.setPostBalance(entry.getBalanceAfter());
                // preBalance = balanceAfter - amount (for credit) or balanceAfter + amount (for
                // debit)
                if (entry.getEntryType() != null) {
                    if (entry.getEntryType().name().equals("CREDIT")) {
                        impact.setPreBalance(entry.getBalanceAfter().subtract(entry.getAmount()));
                    } else {
                        impact.setPreBalance(entry.getBalanceAfter().add(entry.getAmount()));
                    }
                }
            }
        }

        return new ArrayList<>(impactMap.values());
    }

    private List<VoucherDetailDTO> buildVoucherDetails(List<Voucher> vouchers) {
        return vouchers.stream()
                .map(
                        v -> {
                            VoucherDetailDTO vDto = new VoucherDetailDTO();
                            vDto.setId(v.getId());
                            vDto.setVoucherNumber(v.getVoucherNumber());
                            if (v.getBranch() != null) {
                                vDto.setBranchCode(v.getBranch().getBranchCode());
                                vDto.setBranchName(v.getBranch().getBranchName());
                            }
                            vDto.setDrCr(v.getDrCr() != null ? v.getDrCr().name() : null);
                            if (v.getAccount() != null) {
                                vDto.setAccountNumber(v.getAccount().getAccountNumber());
                                vDto.setAccountName(v.getAccount().getAccountName());
                            }
                            if (v.getGlAccount() != null) {
                                vDto.setGlCode(v.getGlAccount().getGlCode());
                            }
                            vDto.setAmount(v.getTransactionAmount());
                            vDto.setAuthFlag(v.getAuthFlag());
                            vDto.setPostFlag(v.getPostFlag());
                            vDto.setCancelFlag(v.getCancelFlag());
                            vDto.setBatchId(v.getBatchId());
                            vDto.setPostingDate(v.getPostingDate());
                            vDto.setScrollNo(v.getScrollNo());
                            vDto.setStatus(v.getStatus() != null ? v.getStatus().name() : null);

                            // Linked ledger entry
                            if (v.getLedgerEntry() != null) {
                                vDto.setLedgerEntryId(v.getLedgerEntry().getId());
                                vDto.setLedgerEntryType(
                                        v.getLedgerEntry().getEntryType() != null
                                                ? v.getLedgerEntry().getEntryType().name()
                                                : null);
                                vDto.setLedgerAmount(v.getLedgerEntry().getAmount());
                                vDto.setBalanceAfter(v.getLedgerEntry().getBalanceAfter());
                            }

                            // Reversal reference
                            if (v.getReversalOfVoucher() != null) {
                                vDto.setReversalOfVoucherId(v.getReversalOfVoucher().getId());
                                vDto.setReversalOfVoucherNumber(
                                        v.getReversalOfVoucher().getVoucherNumber());
                            }

                            // Maker-Checker
                            if (v.getMaker() != null) {
                                vDto.setMakerUsername(v.getMaker().getUsername());
                            }
                            if (v.getChecker() != null) {
                                vDto.setCheckerUsername(v.getChecker().getUsername());
                            }

                            return vDto;
                        })
                .collect(Collectors.toList());
    }

    private List<LedgerEntryDTO> buildLedgerEntryDtos(List<LedgerEntry> entries) {
        return entries.stream()
                .map(
                        e ->
                                LedgerEntryDTO.builder()
                                        .id(e.getId())
                                        .journalId(
                                                e.getJournal() != null
                                                        ? e.getJournal().getId()
                                                        : null)
                                        .entryType(
                                                e.getEntryType() != null
                                                        ? e.getEntryType().name()
                                                        : null)
                                        .glCode(e.getGlAccountCode())
                                        .amount(e.getAmount())
                                        .balanceAfter(e.getBalanceAfter())
                                        .businessDate(e.getBusinessDate())
                                        .batchId(e.getVoucherId())
                                        .voucherId(e.getVoucherId())
                                        .reversalOfEntryId(e.getReversalOfEntryId())
                                        .narration(e.getNarration())
                                        .postingTime(e.getPostingTime())
                                        .build())
                .collect(Collectors.toList());
    }

    private IbtDetailDTO buildIbtDetail(InterBranchTransfer ibt, List<Voucher> allVouchers) {
        IbtDetailDTO ibtDto = new IbtDetailDTO();
        ibtDto.setId(ibt.getId());
        ibtDto.setStatus(ibt.getStatus() != null ? ibt.getStatus().name() : null);
        if (ibt.getFromBranch() != null) {
            ibtDto.setFromBranchCode(ibt.getFromBranch().getBranchCode());
            ibtDto.setFromBranchName(ibt.getFromBranch().getBranchName());
        }
        if (ibt.getToBranch() != null) {
            ibtDto.setToBranchCode(ibt.getToBranch().getBranchCode());
            ibtDto.setToBranchName(ibt.getToBranch().getBranchName());
        }
        ibtDto.setAmount(ibt.getAmount());
        ibtDto.setCurrency(ibt.getCurrency());
        ibtDto.setBusinessDate(ibt.getBusinessDate());
        ibtDto.setSettlementDate(ibt.getSettlementDate());
        ibtDto.setNarration(ibt.getNarration());
        ibtDto.setCreatedByUsername(
                ibt.getCreatedBy() != null ? ibt.getCreatedBy().getUsername() : null);
        ibtDto.setApprovedByUsername(
                ibt.getApprovedBy() != null ? ibt.getApprovedBy().getUsername() : null);
        ibtDto.setFailureReason(ibt.getFailureReason());
        ibtDto.setCreatedAt(ibt.getCreatedAt());
        ibtDto.setUpdatedAt(ibt.getUpdatedAt());

        // Group vouchers by branch for IBT display
        Long fromBranchId = ibt.getFromBranch() != null ? ibt.getFromBranch().getId() : null;
        Long toBranchId = ibt.getToBranch() != null ? ibt.getToBranch().getId() : null;

        List<VoucherDetailDTO> branchAVouchers = new ArrayList<>();
        List<VoucherDetailDTO> branchBVouchers = new ArrayList<>();

        List<VoucherDetailDTO> allVoucherDtos = buildVoucherDetails(allVouchers);
        for (VoucherDetailDTO vDto : allVoucherDtos) {
            // Match by branch code
            boolean isFromBranch =
                    fromBranchId != null
                            && ibt.getFromBranch().getBranchCode() != null
                            && ibt.getFromBranch().getBranchCode().equals(vDto.getBranchCode());
            boolean isToBranch =
                    toBranchId != null
                            && ibt.getToBranch().getBranchCode() != null
                            && ibt.getToBranch().getBranchCode().equals(vDto.getBranchCode());

            if (isFromBranch) {
                branchAVouchers.add(vDto);
            } else if (isToBranch) {
                branchBVouchers.add(vDto);
            } else {
                // Default to branch A if unable to determine
                branchAVouchers.add(vDto);
            }
        }

        ibtDto.setBranchAVouchers(branchAVouchers);
        ibtDto.setBranchBVouchers(branchBVouchers);

        return ibtDto;
    }

    private SuspenseDetailDTO buildSuspenseDetail(SuspenseCase sc) {
        SuspenseDetailDTO sDto = new SuspenseDetailDTO();
        sDto.setId(sc.getId());
        sDto.setReasonCode(sc.getReasonCode());
        sDto.setReasonDetail(sc.getReasonDetail());
        if (sc.getSuspenseAccount() != null) {
            sDto.setSuspenseAccountNumber(sc.getSuspenseAccount().getAccountNumber());
            sDto.setSuspenseAccountName(sc.getSuspenseAccount().getAccountName());
        }
        if (sc.getIntendedAccount() != null) {
            sDto.setIntendedAccountNumber(sc.getIntendedAccount().getAccountNumber());
            sDto.setIntendedAccountName(sc.getIntendedAccount().getAccountName());
        }
        sDto.setAmount(sc.getAmount());
        sDto.setCurrency(sc.getCurrency());
        sDto.setStatus(sc.getStatus());
        sDto.setBusinessDate(sc.getBusinessDate());
        sDto.setResolvedByUsername(
                sc.getResolvedBy() != null ? sc.getResolvedBy().getUsername() : null);
        sDto.setResolutionCheckerUsername(
                sc.getResolutionChecker() != null ? sc.getResolutionChecker().getUsername() : null);
        sDto.setResolutionRemarks(sc.getResolutionRemarks());
        sDto.setResolvedAt(sc.getResolvedAt());
        sDto.setCreatedAt(sc.getCreatedAt());
        sDto.setUpdatedAt(sc.getUpdatedAt());

        // Linked vouchers
        if (sc.getPostedVoucher() != null) {
            sDto.setPostedVoucherNumber(sc.getPostedVoucher().getVoucherNumber());
        }
        if (sc.getSuspenseVoucher() != null) {
            sDto.setSuspenseVoucherNumber(sc.getSuspenseVoucher().getVoucherNumber());
        }
        if (sc.getResolutionVoucher() != null) {
            sDto.setResolutionVoucherNumber(sc.getResolutionVoucher().getVoucherNumber());
        }

        return sDto;
    }

    private AuditTimelineDTO buildAuditTimeline(AuditLog log) {
        return AuditTimelineDTO.builder()
                .id(log.getId())
                .action(log.getAction())
                .timestamp(log.getTimestamp())
                .username(log.getUsername())
                .ipAddress(log.getIpAddress())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .details(log.getDetails())
                .build();
    }

    /**
     * Determine if a transaction is an inter-branch transfer by checking if source and destination
     * accounts belong to different branches.
     */
    private boolean isInterBranchTransaction(Transaction txn) {
        if (txn.getSourceAccount() == null || txn.getDestinationAccount() == null) {
            return false;
        }
        String srcBranch = txn.getSourceAccount().getBranchCode();
        String dstBranch = txn.getDestinationAccount().getBranchCode();
        if (srcBranch == null || dstBranch == null) {
            return false;
        }
        return !srcBranch.equals(dstBranch);
    }
}
