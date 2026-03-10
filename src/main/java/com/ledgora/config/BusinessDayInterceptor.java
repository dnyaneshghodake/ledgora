package com.ledgora.config;

import com.ledgora.common.exception.BusinessDayClosedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that blocks transactional POST requests when the business day is closed. Checks
 * session attribute 'businessDateStatus' and throws BusinessDayClosedException for locked paths
 * when status is CLOSED.
 */
@Component
public class BusinessDayInterceptor implements HandlerInterceptor {

    private static final String[] LOCKED_PATHS = {
        "/vouchers/create",
        "/accounts/create",
        "/transactions/deposit",
        "/transactions/withdraw",
        "/transactions/transfer",
        "/eod/run"
    };

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }

        Object status = session.getAttribute("businessDateStatus");
        if (status == null || !"CLOSED".equals(status.toString())) {
            return true;
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        for (String lockedPath : LOCKED_PATHS) {
            if (path.startsWith(lockedPath)) {
                throw new BusinessDayClosedException(null, "CLOSED");
            }
        }

        return true;
    }
}
