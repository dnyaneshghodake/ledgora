package com.ledgora.teller.service;

import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.TellerStatus;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.common.enums.VaultTransferStatus;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.teller.entity.TellerMaster;
import com.ledgora.teller.entity.TellerSession;
import com.ledgora.teller.entity.VaultMaster;
import com.ledgora.teller.entity.VaultTransfer;
import com.ledgora.teller.repository.TellerMasterRepository;
import com.ledgora.teller.repository.TellerSessionRepository;
import com.ledgora.teller.repository.VaultMasterRepository;
import com.ledgora.teller.repository.VaultTransferRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finacle-grade VaultTransferService.
 *
 * <p>Implements teller↔vault cash movement with RBI dual custody authorization. Uses the existing
 * TransactionService to post accounting entries (do not rewrite the core posting engine).
 *
 * <p>Posting rules (default GL accounts seeded by CustomerAccountSeeder + new TellerDataSeeder
 * extension):
 *
 * <ul>
 *   <li>Teller → Vault: DR Vault Cash (GL 1120), CR Branch Cash (GL 1100)
 *   <li>Vault → Teller: DR Branch Cash (GL 1100), CR Vault Cash (GL 1120)
 * </ul>
 */
@Service
public class VaultTransferService {

    private static final String ENTITY_TELLER = "TELLER";
    private static final String ENTITY_VAULT = "VAULT";

    /** GL codes used for tenant-aware account resolution (not hardcoded account numbers). */
    private static final String GL_CODE_BRANCH_CASH = "1100";

    private static final String GL_CODE_VAULT_CASH = "1120";

    private final TenantService tenantService;
    private final TransactionService transactionService;
    private final TellerMasterRepository tellerMasterRepository;
    private final TellerSessionRepository tellerSessionRepository;
    private final VaultMasterRepository vaultMasterRepository;
    private final VaultTransferRepository vaultTransferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public VaultTransferService(
            TenantService tenantService,
            TransactionService transactionService,
            TellerMasterRepository tellerMasterRepository,
            TellerSessionRepository tellerSessionRepository,
            VaultMasterRepository vaultMasterRepository,
            VaultTransferRepository vaultTransferRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.tenantService = tenantService;
        this.transactionService = transactionService;
        this.tellerMasterRepository = tellerMasterRepository;
        this.tellerSessionRepository = tellerSessionRepository;
        this.vaultMasterRepository = vaultMasterRepository;
        this.vaultTransferRepository = vaultTransferRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /** Initiate transfer from teller to vault (requires dual authorization to complete). */
    @Transactional
    public VaultTransfer initiateTellerToVault(BigDecimal amount, String remarks) {
        validateAmount(amount);

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        LocalDate biz = tenantService.getCurrentBusinessDate(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        User maker = requireCurrentUser();
        TellerMaster tellerMaster = requireTellerMaster(maker.getId(), tenantId);
        TellerSession session = requireOpenSessionWithLock(tellerMaster.getId(), biz);

        if (session.getState() != TellerStatus.OPEN) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Vault transfer allowed only when teller session is OPEN. Current="
                            + session.getState());
        }

        if (session.getCurrentBalance().compareTo(amount) < 0) {
            throw new BusinessException(
                    "TELLER_CASH_INSUFFICIENT",
                    "Insufficient teller cash for vault transfer. Available="
                            + session.getCurrentBalance()
                            + " amount="
                            + amount);
        }

        VaultMaster vault = requireVaultWithLock(tellerMaster.getBranch().getId());

        VaultTransfer vt =
                VaultTransfer.builder()
                        .tenant(tenant)
                        .fromEntity(ENTITY_TELLER)
                        .toEntity(ENTITY_VAULT)
                        .tellerSession(session)
                        .vault(vault)
                        .amount(amount)
                        .status(VaultTransferStatus.INITIATED)
                        .dualAuthUser1(maker)
                        .remarks(remarks)
                        .build();

        VaultTransfer saved = vaultTransferRepository.save(vt);

        auditService.logEvent(
                maker.getId(),
                "VAULT_TRANSFER_INITIATED",
                "VAULT_TRANSFER",
                saved.getId(),
                "Teller→Vault transfer initiated. sessionId="
                        + session.getId()
                        + " vaultId="
                        + vault.getId()
                        + " amount="
                        + amount,
                null);

        return saved;
    }

    /** Initiate transfer from vault to teller (requires dual authorization to complete). */
    @Transactional
    public VaultTransfer initiateVaultToTeller(BigDecimal amount, String remarks) {
        validateAmount(amount);

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        LocalDate biz = tenantService.getCurrentBusinessDate(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        User maker = requireCurrentUser();
        TellerMaster tellerMaster = requireTellerMaster(maker.getId(), tenantId);
        TellerSession session = requireOpenSessionWithLock(tellerMaster.getId(), biz);

        if (session.getState() != TellerStatus.OPEN) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Vault transfer allowed only when teller session is OPEN. Current="
                            + session.getState());
        }

        VaultMaster vault = requireVaultWithLock(tellerMaster.getBranch().getId());
        if (vault.getCurrentBalance().compareTo(amount) < 0) {
            throw new BusinessException(
                    "VAULT_CASH_INSUFFICIENT",
                    "Insufficient vault cash for transfer. Available="
                            + vault.getCurrentBalance()
                            + " amount="
                            + amount);
        }

        VaultTransfer vt =
                VaultTransfer.builder()
                        .tenant(tenant)
                        .fromEntity(ENTITY_VAULT)
                        .toEntity(ENTITY_TELLER)
                        .tellerSession(session)
                        .vault(vault)
                        .amount(amount)
                        .status(VaultTransferStatus.INITIATED)
                        .dualAuthUser1(maker)
                        .remarks(remarks)
                        .build();

        VaultTransfer saved = vaultTransferRepository.save(vt);

        auditService.logEvent(
                maker.getId(),
                "VAULT_TRANSFER_INITIATED",
                "VAULT_TRANSFER",
                saved.getId(),
                "Vault→Teller transfer initiated. sessionId="
                        + session.getId()
                        + " vaultId="
                        + vault.getId()
                        + " amount="
                        + amount,
                null);

        return saved;
    }

    /**
     * Dual custody authorization: second custodian authorizes the INITIATED transfer.
     *
     * <p>On authorization this method:
     *
     * <ul>
     *   <li>Posts the accounting transaction via TransactionService.transfer()
     *   <li>Updates VaultMaster.currentBalance and TellerSession.currentBalance under locks
     *   <li>Marks transfer as AUTHORIZED with authorizedAt
     * </ul>
     */
    @Transactional
    public VaultTransfer authorize(Long vaultTransferId, String remarks) {
        if (vaultTransferId == null) {
            throw new BusinessException("INVALID_REQUEST", "vaultTransferId is required");
        }

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        User checker = requireCurrentUser();

        VaultTransfer vt =
                vaultTransferRepository
                        .findById(vaultTransferId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "VAULT_TRANSFER_NOT_FOUND",
                                                "Vault transfer not found: " + vaultTransferId));

        if (vt.getTenant() == null
                || vt.getTenant().getId() == null
                || !vt.getTenant().getId().equals(tenantId)) {
            throw new BusinessException(
                    "TENANT_MISMATCH",
                    "Vault transfer does not belong to current tenant. transferId="
                            + vaultTransferId);
        }

        if (vt.getStatus() != VaultTransferStatus.INITIATED) {
            throw new BusinessException(
                    "INVALID_STATUS",
                    "Vault transfer can be authorized only when INITIATED. Current="
                            + vt.getStatus());
        }

        if (vt.getDualAuthUser1() != null
                && vt.getDualAuthUser1().getId() != null
                && vt.getDualAuthUser1().getId().equals(checker.getId())) {
            throw new BusinessException(
                    "DUAL_CUSTODY_VIOLATION",
                    "Initiator cannot authorize vault transfer (dual custody required)");
        }

        TellerSession session = vt.getTellerSession();
        if (session == null || session.getId() == null) {
            throw new BusinessException(
                    "INVALID_TRANSFER",
                    "Vault transfer missing teller session. transferId=" + vaultTransferId);
        }

        // Lock teller session and vault for safe balance updates
        TellerSession lockedSession =
                tellerSessionRepository
                        .findByIdWithLock(session.getId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "NO_TELLER_SESSION",
                                                "No teller session found: " + session.getId()));

        if (lockedSession.getTenant() == null
                || lockedSession.getTenant().getId() == null
                || !lockedSession.getTenant().getId().equals(tenantId)) {
            throw new BusinessException(
                    "TENANT_MISMATCH",
                    "Teller session does not belong to current tenant. sessionId="
                            + lockedSession.getId());
        }

        VaultMaster vault = vt.getVault();
        if (vault == null || vault.getBranch() == null || vault.getBranch().getId() == null) {
            throw new BusinessException(
                    "INVALID_TRANSFER",
                    "Vault transfer missing vault/branch. transferId=" + vaultTransferId);
        }
        VaultMaster lockedVault = requireVaultWithLock(vault.getBranch().getId());

        // Validate balances again at authorization time (race-safe)
        BigDecimal amount = vt.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_REQUEST", "Amount must be positive");
        }

        // Determine posting direction
        boolean tellerToVault = ENTITY_TELLER.equalsIgnoreCase(vt.getFromEntity());
        boolean vaultToTeller = ENTITY_VAULT.equalsIgnoreCase(vt.getFromEntity());
        if (!tellerToVault && !vaultToTeller) {
            throw new BusinessException(
                    "INVALID_TRANSFER", "Invalid fromEntity: " + vt.getFromEntity());
        }

        if (tellerToVault && lockedSession.getCurrentBalance().compareTo(amount) < 0) {
            throw new BusinessException(
                    "TELLER_CASH_INSUFFICIENT",
                    "Insufficient teller cash at authorization. Available="
                            + lockedSession.getCurrentBalance()
                            + " amount="
                            + amount);
        }
        if (vaultToTeller && lockedVault.getCurrentBalance().compareTo(amount) < 0) {
            throw new BusinessException(
                    "VAULT_CASH_INSUFFICIENT",
                    "Insufficient vault cash at authorization. Available="
                            + lockedVault.getCurrentBalance()
                            + " amount="
                            + amount);
        }

        // Resolve GL accounts dynamically per tenant (CBS multi-tenant GL isolation)
        String branchCashAccNo = resolveGlAccountNumber(tenantId, GL_CODE_BRANCH_CASH);
        String vaultCashAccNo = resolveGlAccountNumber(tenantId, GL_CODE_VAULT_CASH);

        // Post accounting entry via existing engine (transfer between GL accounts)
        // Teller → Vault: DR Vault Cash GL, CR Branch Cash GL
        // Vault → Teller: DR Branch Cash GL, CR Vault Cash GL
        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType(TransactionType.TRANSFER.name())
                        .amount(amount)
                        .currency(tenant.getBaseCurrency())
                        .channel(TransactionChannel.TELLER.name())
                        .description("Vault Transfer")
                        .narration(
                                (remarks != null && !remarks.isBlank())
                                        ? remarks
                                        : "Vault transfer authorization")
                        .build();
        if (tellerToVault) {
            dto.setSourceAccountNumber(branchCashAccNo);
            dto.setDestinationAccountNumber(vaultCashAccNo);
        } else {
            dto.setSourceAccountNumber(vaultCashAccNo);
            dto.setDestinationAccountNumber(branchCashAccNo);
        }

        Transaction txn = transactionService.transfer(dto);

        // Update operational balances
        if (tellerToVault) {
            lockedSession.setCurrentBalance(lockedSession.getCurrentBalance().subtract(amount));
            lockedVault.setCurrentBalance(lockedVault.getCurrentBalance().add(amount));
        } else {
            lockedVault.setCurrentBalance(lockedVault.getCurrentBalance().subtract(amount));
            lockedSession.setCurrentBalance(lockedSession.getCurrentBalance().add(amount));
        }

        tellerSessionRepository.save(lockedSession);
        // VaultMasterRepository has no generic lock-by-id; we updated lockedVault under lock.
        vaultMasterRepository.save(lockedVault);

        vt.setStatus(VaultTransferStatus.AUTHORIZED);
        vt.setDualAuthUser2(checker);
        vt.setAuthorizedAt(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) {
            vt.setRemarks(remarks);
        }
        VaultTransfer saved = vaultTransferRepository.save(vt);

        auditService.logEvent(
                checker.getId(),
                "VAULT_TRANSFER_AUTHORIZED",
                "VAULT_TRANSFER",
                saved.getId(),
                "Vault transfer authorized. transferId="
                        + saved.getId()
                        + " txnId="
                        + (txn != null ? txn.getId() : null)
                        + " amount="
                        + amount
                        + " direction="
                        + saved.getFromEntity()
                        + "->"
                        + saved.getToEntity(),
                null);

        return saved;
    }

    /** Dual custody rejection: second custodian rejects INITIATED transfer (no posting). */
    @Transactional
    public VaultTransfer reject(Long vaultTransferId, String remarks) {
        if (vaultTransferId == null) {
            throw new BusinessException("INVALID_REQUEST", "vaultTransferId is required");
        }

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);

        User checker = requireCurrentUser();

        VaultTransfer vt =
                vaultTransferRepository
                        .findById(vaultTransferId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "VAULT_TRANSFER_NOT_FOUND",
                                                "Vault transfer not found: " + vaultTransferId));

        if (vt.getTenant() == null
                || vt.getTenant().getId() == null
                || !vt.getTenant().getId().equals(tenantId)) {
            throw new BusinessException(
                    "TENANT_MISMATCH",
                    "Vault transfer does not belong to current tenant. transferId="
                            + vaultTransferId);
        }

        if (vt.getStatus() != VaultTransferStatus.INITIATED) {
            throw new BusinessException(
                    "INVALID_STATUS",
                    "Vault transfer can be rejected only when INITIATED. Current="
                            + vt.getStatus());
        }

        if (vt.getDualAuthUser1() != null
                && vt.getDualAuthUser1().getId() != null
                && vt.getDualAuthUser1().getId().equals(checker.getId())) {
            throw new BusinessException(
                    "DUAL_CUSTODY_VIOLATION",
                    "Initiator cannot reject vault transfer (dual custody required)");
        }

        vt.setStatus(VaultTransferStatus.REJECTED);
        vt.setDualAuthUser2(checker);
        vt.setAuthorizedAt(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) {
            vt.setRemarks(remarks);
        }

        VaultTransfer saved = vaultTransferRepository.save(vt);

        auditService.logEvent(
                checker.getId(),
                "VAULT_TRANSFER_REJECTED",
                "VAULT_TRANSFER",
                saved.getId(),
                "Vault transfer rejected. transferId=" + saved.getId(),
                null);

        return saved;
    }

    /**
     * Resolve GL account number by GL code for a specific tenant. CBS multi-tenant rule: each
     * tenant may have different account numbers for the same GL code. Matches the pattern used by
     * TransactionService.resolveCashGlAccount().
     */
    private String resolveGlAccountNumber(Long tenantId, String glCode) {
        return accountRepository
                .findFirstByTenantIdAndGlAccountCode(tenantId, glCode)
                .map(a -> a.getAccountNumber())
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "GL_ACCOUNT_NOT_FOUND",
                                        "GL account with code "
                                                + glCode
                                                + " not found for tenant "
                                                + tenantId
                                                + ". CBS requires valid GL mapping for vault transfers."));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_REQUEST", "Amount must be positive");
        }
    }

    private VaultMaster requireVaultWithLock(Long branchId) {
        return vaultMasterRepository
                .findByBranchIdWithLock(branchId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "NO_VAULT",
                                        "No vault configured for branchId=" + branchId));
    }

    private TellerSession requireOpenSessionWithLock(Long tellerId, LocalDate businessDate) {
        TellerSession session =
                tellerSessionRepository
                        .findByTellerIdAndBusinessDate(tellerId, businessDate)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "NO_TELLER_SESSION",
                                                "No teller session found for business date "
                                                        + businessDate));

        return tellerSessionRepository
                .findByIdWithLock(session.getId())
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "NO_TELLER_SESSION",
                                        "No teller session found: " + session.getId()));
    }

    private TellerMaster requireTellerMaster(Long userId, Long tenantId) {
        TellerMaster tm =
                tellerMasterRepository
                        .findByUserIdAndTenantId(userId, tenantId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "TELLER_NOT_CONFIGURED",
                                                "No teller master configured for user " + userId));

        if (tm.getActiveFlag() == null || !tm.getActiveFlag()) {
            throw new BusinessException("TELLER_INACTIVE", "Teller is inactive for user " + userId);
        }

        return tm;
    }

    private User requireCurrentUser() {
        try {
            org.springframework.security.core.Authentication auth =
                    SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                throw new BusinessException("IDENTITY_REQUIRED", "Authentication is missing");
            }
            User u = userRepository.findByUsername(auth.getName()).orElse(null);
            if (u == null) {
                throw new BusinessException(
                        "IDENTITY_REQUIRED", "User not found for principal: " + auth.getName());
            }
            return u;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("IDENTITY_REQUIRED", "Failed to resolve current user");
        }
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }
}
