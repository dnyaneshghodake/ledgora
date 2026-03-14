package com.ledgora.teller.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.CashDifferenceType;
import com.ledgora.common.enums.TellerStatus;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.teller.dto.DenominationEntry;
import com.ledgora.teller.dto.TellerCloseRequest;
import com.ledgora.teller.dto.TellerOpenRequest;
import com.ledgora.teller.entity.CashDifferenceLog;
import com.ledgora.teller.entity.TellerMaster;
import com.ledgora.teller.entity.TellerSession;
import com.ledgora.teller.entity.TellerSessionDenomination;
import com.ledgora.teller.repository.CashDifferenceLogRepository;
import com.ledgora.teller.repository.TellerMasterRepository;
import com.ledgora.teller.repository.TellerSessionDenominationRepository;
import com.ledgora.teller.repository.TellerSessionRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TellerSessionService {

    private static final String EVENT_OPENING = "OPENING";
    private static final String EVENT_CLOSING = "CLOSING";

    private final TenantService tenantService;
    private final TellerMasterRepository tellerMasterRepository;
    private final TellerSessionRepository tellerSessionRepository;
    private final TellerSessionDenominationRepository tellerSessionDenominationRepository;
    private final CashDifferenceLogRepository cashDifferenceLogRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public TellerSessionService(
            TenantService tenantService,
            TellerMasterRepository tellerMasterRepository,
            TellerSessionRepository tellerSessionRepository,
            TellerSessionDenominationRepository tellerSessionDenominationRepository,
            CashDifferenceLogRepository cashDifferenceLogRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.tenantService = tenantService;
        this.tellerMasterRepository = tellerMasterRepository;
        this.tellerSessionRepository = tellerSessionRepository;
        this.tellerSessionDenominationRepository = tellerSessionDenominationRepository;
        this.cashDifferenceLogRepository = cashDifferenceLogRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /** Maker step: request teller session opening for current business date. */
    @Transactional
    public TellerSession requestOpen(TellerOpenRequest req) {
        validateOpenRequest(req);

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);
        LocalDate bizDate = tenantService.getCurrentBusinessDate(tenantId);

        User maker = requireCurrentUser();
        TellerMaster tellerMaster = requireTellerMaster(maker.getId(), tenantId);

        tellerSessionRepository
                .findByTellerIdAndBusinessDate(tellerMaster.getId(), bizDate)
                .ifPresent(
                        existing -> {
                            if (existing.getState() == TellerStatus.CLOSED) {
                                throw new BusinessException(
                                        "TELLER_SESSION_CLOSED",
                                        "Teller session already closed for business date "
                                                + bizDate
                                                + ". Cannot reopen.");
                            }
                            throw new BusinessException(
                                    "TELLER_SESSION_EXISTS",
                                    "Teller session already exists for business date "
                                            + bizDate
                                            + " state="
                                            + existing.getState());
                        });

        BigDecimal denomTotal = sumDenominations(req.getDenominations());
        if (denomTotal.compareTo(req.getOpeningBalance()) != 0) {
            throw new BusinessException(
                    "DENOMINATION_MISMATCH",
                    "Opening denominations total "
                            + denomTotal
                            + " does not match openingBalance "
                            + req.getOpeningBalance());
        }

        TellerSession session =
                TellerSession.builder()
                        .tenant(tenant)
                        .teller(tellerMaster)
                        .branch(tellerMaster.getBranch())
                        .businessDate(bizDate)
                        .openingBalance(req.getOpeningBalance())
                        .currentBalance(req.getOpeningBalance())
                        .totalCreditToday(BigDecimal.ZERO)
                        .totalDebitToday(BigDecimal.ZERO)
                        .state(TellerStatus.OPEN_REQUESTED)
                        .openedBy(maker)
                        .build();

        TellerSession saved = tellerSessionRepository.save(session);

        persistSessionDenominations(saved, EVENT_OPENING, req.getDenominations());

        tellerMaster.setStatus(TellerStatus.OPEN_REQUESTED);
        tellerMasterRepository.save(tellerMaster);

        auditService.logEvent(
                maker.getId(),
                "TELLER_SESSION_OPEN_REQUEST",
                "TELLER_SESSION",
                saved.getId(),
                "Teller session open requested. tellerMasterId="
                        + tellerMaster.getId()
                        + " businessDate="
                        + bizDate
                        + " openingBalance="
                        + req.getOpeningBalance(),
                null);

        return saved;
    }

    /** Checker step: authorize teller session opening (maker-checker enforced). */
    @Transactional
    public TellerSession authorizeOpen(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException("INVALID_REQUEST", "sessionId is required");
        }

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);

        User checker = requireCurrentUser();

        TellerSession session =
                tellerSessionRepository
                        .findByIdWithLock(sessionId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "NO_TELLER_SESSION",
                                                "No teller session found: " + sessionId));

        // Tenant isolation: session must belong to the current tenant
        if (session.getTenant() == null
                || !session.getTenant().getId().equals(tenantId)) {
            throw new BusinessException(
                    "TENANT_MISMATCH",
                    "Session does not belong to current tenant. sessionId=" + sessionId);
        }

        if (session.getState() != TellerStatus.OPEN_REQUESTED) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Authorize open allowed only when session is OPEN_REQUESTED. Current="
                            + session.getState());
        }

        if (session.getOpenedBy() != null
                && session.getOpenedBy().getId() != null
                && session.getOpenedBy().getId().equals(checker.getId())) {
            throw new BusinessException(
                    "MAKER_CHECKER_VIOLATION",
                    "Teller cannot authorize own session open request");
        }

        session.setState(TellerStatus.OPEN);
        session.setAuthorizedBy(checker);
        session.setOpenedAt(LocalDateTime.now());
        TellerSession saved = tellerSessionRepository.save(session);

        TellerMaster tellerMaster = session.getTeller();
        tellerMaster.setStatus(TellerStatus.OPEN);
        tellerMasterRepository.save(tellerMaster);

        auditService.logEvent(
                checker.getId(),
                "TELLER_SESSION_OPEN_AUTHORIZED",
                "TELLER_SESSION",
                saved.getId(),
                "Teller session open authorized. tellerMasterId="
                        + tellerMaster.getId()
                        + " businessDate="
                        + saved.getBusinessDate(),
                null);

        return saved;
    }

    /** Maker step: request teller session closure for current business date. */
    @Transactional
    public TellerSession requestClose(TellerCloseRequest req) {
        validateCloseRequest(req);

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        LocalDate bizDate = tenantService.getCurrentBusinessDate(tenantId);

        User maker = requireCurrentUser();
        TellerMaster tellerMaster = requireTellerMaster(maker.getId(), tenantId);

        TellerSession session = requireSessionWithLock(tellerMaster.getId(), bizDate);

        if (session.getState() != TellerStatus.OPEN && session.getState() != TellerStatus.SUSPENDED) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Close request allowed only when session is OPEN or SUSPENDED. Current="
                            + session.getState());
        }

        BigDecimal denomTotal = sumDenominations(req.getDenominations());
        if (denomTotal.compareTo(req.getDeclaredAmount()) != 0) {
            throw new BusinessException(
                    "DENOMINATION_MISMATCH",
                    "Closing denominations total "
                            + denomTotal
                            + " does not match declaredAmount "
                            + req.getDeclaredAmount());
        }

        persistSessionDenominations(session, EVENT_CLOSING, req.getDenominations());

        BigDecimal systemAmount = session.getCurrentBalance();
        BigDecimal declaredAmount = req.getDeclaredAmount();
        BigDecimal diff = declaredAmount.subtract(systemAmount);

        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal absDiff = diff.abs();
            CashDifferenceType type =
                    diff.compareTo(BigDecimal.ZERO) < 0 ? CashDifferenceType.SHORT : CashDifferenceType.EXCESS;

            cashDifferenceLogRepository.save(
                    CashDifferenceLog.builder()
                            .session(session)
                            .declaredAmount(declaredAmount)
                            .systemAmount(systemAmount)
                            .difference(absDiff)
                            .type(type)
                            .resolvedFlag(false)
                            .build());
        }

        session.setState(TellerStatus.CLOSING_REQUESTED);
        session.setClosedBy(maker);
        TellerSession saved = tellerSessionRepository.save(session);

        TellerMaster tm = saved.getTeller();
        tm.setStatus(TellerStatus.CLOSING_REQUESTED);
        tellerMasterRepository.save(tm);

        auditService.logEvent(
                maker.getId(),
                "TELLER_SESSION_CLOSE_REQUEST",
                "TELLER_SESSION",
                saved.getId(),
                "Teller session close requested. tellerMasterId="
                        + tellerMaster.getId()
                        + " businessDate="
                        + bizDate
                        + " declaredAmount="
                        + declaredAmount
                        + " systemAmount="
                        + systemAmount,
                null);

        return saved;
    }

    /** Checker step: authorize teller session closure (maker-checker + reconciliation enforced). */
    @Transactional
    public TellerSession authorizeClose(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException("INVALID_REQUEST", "sessionId is required");
        }

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);

        User checker = requireCurrentUser();

        TellerSession session =
                tellerSessionRepository
                        .findByIdWithLock(sessionId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "NO_TELLER_SESSION",
                                                "No teller session found: " + sessionId));

        // Tenant isolation: session must belong to the current tenant
        if (session.getTenant() == null
                || !session.getTenant().getId().equals(tenantId)) {
            throw new BusinessException(
                    "TENANT_MISMATCH",
                    "Session does not belong to current tenant. sessionId=" + sessionId);
        }

        if (session.getState() != TellerStatus.CLOSING_REQUESTED) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Authorize close allowed only when session is CLOSING_REQUESTED. Current="
                            + session.getState());
        }

        if (session.getClosedBy() != null
                && session.getClosedBy().getId() != null
                && session.getClosedBy().getId().equals(checker.getId())) {
            throw new BusinessException(
                    "MAKER_CHECKER_VIOLATION",
                    "Teller cannot authorize own session close request");
        }

        List<CashDifferenceLog> unresolved =
                cashDifferenceLogRepository.findBySessionIdAndResolvedFlagFalse(sessionId);
        if (unresolved != null && !unresolved.isEmpty()) {
            throw new BusinessException(
                    "CASH_DIFF_UNRESOLVED",
                    "Cannot close teller session: cash difference unresolved. sessionId=" + sessionId);
        }

        session.setState(TellerStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        TellerSession saved = tellerSessionRepository.save(session);

        TellerMaster tellerMaster = saved.getTeller();
        tellerMaster.setStatus(TellerStatus.CLOSED);
        tellerMasterRepository.save(tellerMaster);

        auditService.logEvent(
                checker.getId(),
                "TELLER_SESSION_CLOSE_AUTHORIZED",
                "TELLER_SESSION",
                saved.getId(),
                "Teller session close authorized. tellerMasterId="
                        + tellerMaster.getId()
                        + " businessDate="
                        + saved.getBusinessDate(),
                null);

        return saved;
    }

    @Transactional
    public TellerSession suspend() {
        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        LocalDate bizDate = tenantService.getCurrentBusinessDate(tenantId);

        User user = requireCurrentUser();
        TellerMaster tellerMaster = requireTellerMaster(user.getId(), tenantId);
        TellerSession session = requireSessionWithLock(tellerMaster.getId(), bizDate);

        if (session.getState() != TellerStatus.OPEN) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Suspend allowed only when session is OPEN. Current=" + session.getState());
        }

        session.setState(TellerStatus.SUSPENDED);
        TellerSession saved = tellerSessionRepository.save(session);

        TellerMaster tm = saved.getTeller();
        tm.setStatus(TellerStatus.SUSPENDED);
        tellerMasterRepository.save(tm);

        auditService.logEvent(
                user.getId(),
                "TELLER_SESSION_SUSPENDED",
                "TELLER_SESSION",
                saved.getId(),
                "Teller session suspended. tellerMasterId="
                        + tm.getId()
                        + " businessDate="
                        + saved.getBusinessDate(),
                null);

        return saved;
    }

    @Transactional
    public TellerSession resume() {
        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        LocalDate bizDate = tenantService.getCurrentBusinessDate(tenantId);

        User user = requireCurrentUser();
        TellerMaster tellerMaster = requireTellerMaster(user.getId(), tenantId);
        TellerSession session = requireSessionWithLock(tellerMaster.getId(), bizDate);

        if (session.getState() != TellerStatus.SUSPENDED) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Resume allowed only when session is SUSPENDED. Current=" + session.getState());
        }

        session.setState(TellerStatus.OPEN);
        TellerSession saved = tellerSessionRepository.save(session);

        TellerMaster tm = saved.getTeller();
        tm.setStatus(TellerStatus.OPEN);
        tellerMasterRepository.save(tm);

        auditService.logEvent(
                user.getId(),
                "TELLER_SESSION_RESUMED",
                "TELLER_SESSION",
                saved.getId(),
                "Teller session resumed. tellerMasterId="
                        + tm.getId()
                        + " businessDate="
                        + saved.getBusinessDate(),
                null);

        return saved;
    }

    private void validateOpenRequest(TellerOpenRequest req) {
        if (req == null) {
            throw new BusinessException("INVALID_REQUEST", "Request must not be null");
        }
        if (req.getOpeningBalance() == null || req.getOpeningBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_REQUEST", "Opening balance must be >= 0");
        }
        if (req.getDenominations() == null || req.getDenominations().isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "Denominations are required");
        }
    }

    private void validateCloseRequest(TellerCloseRequest req) {
        if (req == null) {
            throw new BusinessException("INVALID_REQUEST", "Request must not be null");
        }
        if (req.getDeclaredAmount() == null || req.getDeclaredAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_REQUEST", "Declared amount must be >= 0");
        }
        if (req.getDenominations() == null || req.getDenominations().isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "Denominations are required");
        }
    }

    private BigDecimal sumDenominations(List<DenominationEntry> entries) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DenominationEntry e : entries) {
            if (e == null || e.getDenominationValue() == null) {
                throw new BusinessException("INVALID_DENOMINATION", "Denomination value is required");
            }
            if (e.getDenominationValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "INVALID_DENOMINATION", "Denomination value must be positive");
            }
            if (e.getCount() == null || e.getCount() < 0) {
                throw new BusinessException("INVALID_DENOMINATION", "Denomination count must be >= 0");
            }
            BigDecimal lineTotal =
                    e.getDenominationValue().multiply(BigDecimal.valueOf(e.getCount()));
            sum = sum.add(lineTotal);
        }
        return sum;
    }

    private void persistSessionDenominations(
            TellerSession session, String eventType, List<DenominationEntry> denominations) {
        for (DenominationEntry e : denominations) {
            if (e.getCount() == null || e.getCount() == 0) {
                continue;
            }
            tellerSessionDenominationRepository.save(
                    TellerSessionDenomination.builder()
                            .session(session)
                            .eventType(eventType)
                            .denominationValue(e.getDenominationValue())
                            .count(e.getCount())
                            // totalAmount computed in @PrePersist
                            .totalAmount(BigDecimal.ZERO)
                            .build());
        }
    }

    private TellerSession requireSessionWithLock(Long tellerId, LocalDate businessDate) {
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
