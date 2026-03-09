package com.ledgora.security;

import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.TenantScope;
import com.ledgora.tenant.context.TenantContextHolder;
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
    private final UserRepository userRepository;

    public CustomAuthenticationSuccessHandler(JwtTokenProvider jwtTokenProvider,
                                              UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
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
        session.setAttribute("isOperations",    roles.contains("ROLE_OPERATIONS"));
        session.setAttribute("isAtmSystem",     roles.contains("ROLE_ATM_SYSTEM"));

        // Resolve tenant context from User entity and generate JWT with tenant claims
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        Long tenantId = null;
        String tenantScopeStr = TenantScope.SINGLE.name();
        if (user != null && user.getTenant() != null) {
            tenantId = user.getTenant().getId();
            tenantScopeStr = user.getTenantScope() != null ? user.getTenantScope().name() : TenantScope.SINGLE.name();
            // Set tenant context for the current request
            TenantContextHolder.setTenantId(tenantId);
            // Store tenant info in session for JSP access
            session.setAttribute("tenantId", tenantId);
            session.setAttribute("tenantName", user.getTenant().getTenantName());
            session.setAttribute("tenantScope", tenantScopeStr);
            // Store business date and day status in session
            if (user.getTenant().getCurrentBusinessDate() != null) {
                session.setAttribute("businessDate", user.getTenant().getCurrentBusinessDate().toString());
            }
            if (user.getTenant().getDayStatus() != null) {
                session.setAttribute("businessDateStatus", user.getTenant().getDayStatus().name());
            }
        }
        // Store branch info in session
        if (user != null) {
            if (user.getBranch() != null) {
                session.setAttribute("branchCode", user.getBranch().getBranchCode());
                session.setAttribute("branchName", user.getBranch().getName());
            } else if (user.getBranchCode() != null) {
                session.setAttribute("branchCode", user.getBranchCode());
            }
        }

        // Generate JWT with tenant claims so TenantContextHolder is populated on every request
        String token;
        if (tenantId != null) {
            token = jwtTokenProvider.generateTokenWithTenant(authentication, tenantId, tenantScopeStr);
        } else {
            token = jwtTokenProvider.generateToken(authentication);
        }
        session.setAttribute("jwt_token", token);

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
