package com.ledgora.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    public CustomAuthenticationSuccessHandler(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Store username in session so the navbar renders (header.jsp checks sessionScope.username)
        session.setAttribute("username", userDetails.getUsername());

        // Store user roles in session for role-based JSP rendering (header.jsp / dashboard.jsp)
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        session.setAttribute("userRoles", roles);

        // Convenience booleans for simpler JSP checks (e.g. sessionScope.isAdmin)
        session.setAttribute("isAdmin",    roles.contains("ROLE_ADMIN"));
        session.setAttribute("isManager",  roles.contains("ROLE_MANAGER"));
        session.setAttribute("isTeller",   roles.contains("ROLE_TELLER"));
        session.setAttribute("isCustomer", roles.contains("ROLE_CUSTOMER"));

        // CBS-specific role flags
        session.setAttribute("isMaker",         roles.contains("ROLE_MAKER"));
        session.setAttribute("isChecker",       roles.contains("ROLE_CHECKER"));
        session.setAttribute("isBranchManager", roles.contains("ROLE_BRANCH_MANAGER"));
        session.setAttribute("isTenantAdmin",   roles.contains("ROLE_TENANT_ADMIN"));
        session.setAttribute("isSuperAdmin",    roles.contains("ROLE_SUPER_ADMIN"));
        session.setAttribute("isAuditor",       roles.contains("ROLE_AUDITOR"));
        session.setAttribute("isFinance",       roles.contains("ROLE_FINANCE"));

        // Generate and store JWT token in session for API calls
        String token = jwtTokenProvider.generateToken(authentication);
        session.setAttribute("jwt_token", token);

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
