package com.ledgora.config;

import com.ledgora.security.CustomUserDetailsService;
import com.ledgora.security.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** Known insecure default — used only to detect misconfiguration. */
    private static final String DEV_JWT_SECRET =
            "ledgora-dev-only-secret-key-must-be-at-least-256-bits-long";

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.ledgora.security.CustomAuthenticationSuccessHandler
            customAuthenticationSuccessHandler;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.previous-secret:}")
    private String jwtPreviousSecret;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            com.ledgora.security.CustomAuthenticationSuccessHandler
                    customAuthenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    /**
     * RBI-F2: Validate JWT key configuration at startup.
     * <ul>
     *   <li>Fail-fast if current secret is blank</li>
     *   <li>Warn if using dev-only default</li>
     *   <li>Log rotation state if previous-secret is configured</li>
     *   <li>Block if current == previous (misconfiguration)</li>
     *   <li>Validate minimum key length (32 bytes for HS256)</li>
     * </ul>
     */
    @PostConstruct
    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.error("SECURITY: app.jwt.secret is not set. Set JWT_SECRET environment variable.");
            throw new IllegalStateException(
                    "app.jwt.secret must be configured. Set JWT_SECRET env var.");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 characters (256 bits) for HS256.");
        }
        if (jwtSecret.equals(DEV_JWT_SECRET)) {
            log.warn(
                    "SECURITY WARNING: Using dev-only JWT secret. "
                            + "Override via JWT_SECRET env var before production deployment.");
        }
        // Rotation state logging
        if (jwtPreviousSecret != null && !jwtPreviousSecret.isBlank()) {
            if (jwtPreviousSecret.equals(jwtSecret)) {
                throw new IllegalStateException(
                        "app.jwt.previous-secret must differ from app.jwt.secret. "
                                + "Both keys are identical — rotation is not configured correctly.");
            }
            log.info(
                    "JWT KEY ROTATION ACTIVE: previous-secret is configured. "
                            + "Tokens signed with the previous key will be accepted during the grace period.");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(
                        csrf -> {
                            csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                    .csrfTokenRequestHandler(
                                            new CsrfTokenRequestAttributeHandler());
                            // Only exclude H2 console from CSRF when it is actually enabled
                            if (h2ConsoleEnabled) {
                                csrf.ignoringRequestMatchers("/h2-console/**");
                            }
                        })
                .authorizeHttpRequests(
                        auth -> {
                            auth.dispatcherTypeMatchers(
                                            DispatcherType.FORWARD, DispatcherType.ERROR)
                                    .permitAll()
                                    .requestMatchers("/", "/login", "/register")
                                    .permitAll()
                                    .requestMatchers("/resources/**", "/css/**", "/js/**")
                                    .permitAll()
                                    // RBI-F1: H2 console requires ADMIN role (never permitAll)
                                    .requestMatchers("/h2-console/**")
                                    .hasRole("ADMIN")
                                    .requestMatchers("/admin/**")
                                    .hasRole("ADMIN")
                                    .requestMatchers("/audit/validation")
                                    .hasAnyRole("AUDITOR", "ADMIN", "SUPER_ADMIN")
                                    .requestMatchers("/customers/**")
                                    .authenticated()
                                    .requestMatchers("/approvals/**")
                                    .authenticated()
                                    .requestMatchers("/reports/**")
                                    .authenticated()
                                    .anyRequest()
                                    .authenticated();
                        })
                .formLogin(
                        form ->
                                form.loginPage("/login")
                                        .loginProcessingUrl("/perform_login")
                                        .successHandler(customAuthenticationSuccessHandler)
                                        .failureUrl("/login?error=true")
                                        .permitAll())
                .logout(
                        logout ->
                                logout.logoutUrl("/logout")
                                        .logoutSuccessUrl("/login?logout=true")
                                        .invalidateHttpSession(true)
                                        .deleteCookies("JSESSIONID")
                                        .permitAll())
                .headers(
                        headers ->
                                headers.frameOptions(frame -> frame.sameOrigin())
                                        .contentTypeOptions(contentType -> {})
                                        .httpStrictTransportSecurity(
                                                hsts ->
                                                        hsts.includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000))
                                        .referrerPolicy(
                                                referrer ->
                                                        referrer.policy(
                                                                org.springframework.security.web
                                                                        .header.writers
                                                                        .ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                        .permissionsPolicy(
                                                permissions ->
                                                        permissions.policy(
                                                                "geolocation=(), microphone=(), camera=()")))
                .sessionManagement(
                        session ->
                                session.sessionFixation(fix -> fix.migrateSession())
                                        // RBI-F5: Limit concurrent sessions per user to 1.
                                        // Second login from another browser/machine is blocked.
                                        .maximumSessions(1)
                                        .maxSessionsPreventsLogin(true));

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
