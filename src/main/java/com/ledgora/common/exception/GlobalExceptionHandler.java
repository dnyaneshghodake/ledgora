package com.ledgora.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Global exception handler for CBS application.
 * Routes all exceptions to a centralized error page with user-friendly messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Access Denied",
                "You do not have permission to access this resource. Please contact your administrator.",
                "403");
        log.warn("Access denied: {} - URI: {}", ex.getMessage(), request.getRequestURI());
        return "error/error";
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessException(BusinessException ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Business Rule Violation",
                ex.getMessage(), ex.getErrorCode());
        log.warn("Business exception: {} - Code: {}", ex.getMessage(), ex.getErrorCode());
        return "error/error";
    }

    @ExceptionHandler(InvalidTransactionAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidAmount(InvalidTransactionAmountException ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Invalid Transaction Amount",
                ex.getMessage(), "INVALID_AMOUNT");
        log.warn("Invalid transaction amount: {}", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(ScriptInjectionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleScriptInjection(ScriptInjectionException ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Invalid Input",
                "Your input contains invalid characters. Please remove any special characters and try again.",
                "SCRIPT_INJECTION");
        log.warn("Script injection attempt detected: {} - URI: {}", ex.getMessage(), request.getRequestURI());
        return "error/error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Validation Error",
                ex.getMessage(), "VALIDATION_ERROR");
        log.warn("Validation error: {}", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInsufficientBalance(InsufficientBalanceException ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Insufficient Balance",
                ex.getMessage(), "INSUFFICIENT_BALANCE");
        log.warn("Insufficient balance: {}", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(BusinessDayClosedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleEodClosed(BusinessDayClosedException ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Business Day Closed",
                "The business day is closed. No transactions are allowed until the next business day is opened.",
                "EOD_CLOSED");
        log.warn("EOD closed exception: {}", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(TenantIsolationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleTenantIsolation(TenantIsolationException ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Tenant Isolation Violation",
                "You are not authorized to access data from another tenant. This incident has been logged.",
                "TENANT_VIOLATION");
        log.error("Tenant isolation violation: {}", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericError(Throwable ex, Model model, HttpServletRequest request) {
        populateErrorModel(model, request, "Unexpected Error",
                "An unexpected error occurred. Please try again or contact support if the problem persists.",
                "INTERNAL_ERROR");
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return "error/error";
    }

    private void populateErrorModel(Model model, HttpServletRequest request,
                                     String title, String message, String errorCode) {
        String correlationId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        model.addAttribute("errorCode", errorCode);
        model.addAttribute("correlationId", correlationId);
        model.addAttribute("timestamp", timestamp);

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object tenant = session.getAttribute("tenantName");
            Object branch = session.getAttribute("branchCode");
            Object bizDate = session.getAttribute("businessDate");
            model.addAttribute("tenantName", tenant != null ? tenant : "N/A");
            model.addAttribute("branchCode", branch != null ? branch : "N/A");
            model.addAttribute("businessDate", bizDate != null ? bizDate : "N/A");
        } else {
            model.addAttribute("tenantName", "N/A");
            model.addAttribute("branchCode", "N/A");
            model.addAttribute("businessDate", "N/A");
        }
    }
}
