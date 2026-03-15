package com.ledgora.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * JWT Token Provider with dual-key rotation support.
 *
 * <p>RBI IT Framework — Key Management:
 *
 * <ul>
 *   <li>New tokens are always signed with the CURRENT key ({@code app.jwt.secret})
 *   <li>Validation tries CURRENT key first, then PREVIOUS key ({@code app.jwt.previous-secret})
 *   <li>Each token includes a {@code kid} (key ID) header for audit traceability
 *   <li>Tokens validated against the PREVIOUS key are logged for rotation monitoring
 * </ul>
 *
 * <p>Rotation procedure:
 *
 * <ol>
 *   <li>Set {@code app.jwt.previous-secret} = current secret value
 *   <li>Set {@code app.jwt.secret} = new secret value
 *   <li>Restart application — existing sessions validated via previous key
 *   <li>After {@code app.jwt.expiration} (default 24h), all old tokens expire naturally
 *   <li>Remove {@code app.jwt.previous-secret} (optional cleanup)
 * </ol>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** Key ID for the current signing key. */
    private static final String CURRENT_KID = "current";

    /** Key ID for the previous (rotation grace period) key. */
    private static final String PREVIOUS_KID = "previous";

    @Value("${app.jwt.secret:ledgora-secret-key-must-be-at-least-256-bits-long-for-hs256}")
    private String jwtSecret;

    /**
     * Previous JWT secret for graceful key rotation. When set, tokens signed with this key are
     * still accepted during the rotation grace period (until they expire naturally). Set to empty
     * string or omit to disable rotation fallback.
     */
    @Value("${app.jwt.previous-secret:}")
    private String jwtPreviousSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getPreviousSigningKey() {
        if (jwtPreviousSecret == null || jwtPreviousSecret.isBlank()) {
            return null;
        }
        byte[] keyBytes = jwtPreviousSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .header()
                .add("kid", CURRENT_KID)
                .and()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** Generate token with tenant_id and tenant_scope claims for multi-tenant support. */
    public String generateTokenWithTenant(
            Authentication authentication, Long tenantId, String tenantScope) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .header()
                .add("kid", CURRENT_KID)
                .and()
                .subject(userDetails.getUsername())
                .claims()
                .add(
                        Map.of(
                                "tenant_id", tenantId,
                                "tenant_scope", tenantScope))
                .and()
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    /** Extract tenant_id from JWT token. */
    public Long getTenantIdFromToken(String token) {
        Claims claims = getClaims(token);
        Object tenantId = claims.get("tenant_id");
        if (tenantId instanceof Number) {
            return ((Number) tenantId).longValue();
        }
        return null;
    }

    /** Extract tenant_scope from JWT token. */
    public String getTenantScopeFromToken(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("tenant_scope");
    }

    /**
     * Parse claims using dual-key strategy: try current key first, then previous key. Tokens
     * validated against the previous key are logged for rotation monitoring.
     */
    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            // Try previous key if configured (rotation grace period)
            SecretKey previousKey = getPreviousSigningKey();
            if (previousKey != null) {
                try {
                    Claims claims =
                            Jwts.parser()
                                    .verifyWith(previousKey)
                                    .build()
                                    .parseSignedClaims(token)
                                    .getPayload();
                    log.info(
                            "JWT validated with PREVIOUS key (rotation grace period) for user: {}",
                            claims.getSubject());
                    return claims;
                } catch (JwtException e2) {
                    // Both keys failed — throw the original exception
                }
            }
            throw e;
        }
    }

    /**
     * Validate token using dual-key strategy. Tries current key first, falls back to previous key
     * during rotation grace period.
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
