package com.ledgora.security;

import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Extract tenant context from JWT and set in TenantContextHolder
                Long tenantId = jwtTokenProvider.getTenantIdFromToken(jwt);
                if (tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                    // Also store in session for JSP access
                    HttpSession session = request.getSession(true);
                    session.setAttribute("tenantId", tenantId);
                    session.setAttribute(
                            "tenantScope", jwtTokenProvider.getTenantScopeFromToken(jwt));
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear tenant context after request to prevent ThreadLocal leakage
            // across pooled threads (CWE-212: Improper Cross-boundary Removal of Data)
            TenantContextHolder.clear();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // Check Authorization header first
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Check session for JSP-based auth
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object token = session.getAttribute("jwt_token");
            if (token != null) {
                return token.toString();
            }
        }

        return null;
    }
}
