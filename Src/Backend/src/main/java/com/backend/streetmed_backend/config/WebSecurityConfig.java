package com.backend.streetmed_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration for StreetMed Backend
 * Works with existing RequestCorsFilter and OptionsRequestFilter
 * Supports both local (TLS) and production environments
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("╔══════════════════════════════════════════════════════════════╗");
        logger.info("║         StreetMed Security Configuration Loading            ║");
        logger.info("╚══════════════════════════════════════════════════════════════╝");

        http
                // Enable CORS with our configuration
                .cors(cors -> {
                    cors.configurationSource(corsConfigurationSource());
                    logger.info("✓ CORS enabled with configuration from properties");
                })

                // Disable CSRF for REST API
                .csrf(csrf -> {
                    csrf.disable();
                    logger.info("✓ CSRF disabled for REST API");
                })

                // Stateless session (we use custom ECDH auth)
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    logger.info("✓ Session management: STATELESS");
                })

                // Configure authorization
                .authorizeHttpRequests(auth -> {
                    logger.info("Configuring endpoint authorization...");

                    auth
                            // ===== PUBLIC TLS TEST ENDPOINTS =====
                            .requestMatchers(
                                    "/api/test/tls/**",
                                    "/api/test/tls/status",
                                    "/api/test/tls/health",
                                    "/api/test/tls/cert-test"
                            ).permitAll()

                            // ===== PUBLIC SECURITY/ECDH ENDPOINTS =====
                            .requestMatchers(
                                    "/api/security/initiate-handshake",
                                    "/api/security/complete-handshake",
                                    "/api/security/**"
                            ).permitAll()

                            // ===== PUBLIC AUTH ENDPOINTS =====
                            .requestMatchers(
                                    "/api/auth/login",
                                    "/api/auth/register",
                                    "/api/auth/logout",
                                    "/api/auth/**"
                            ).permitAll()

                            // ===== PUBLIC GUEST ENDPOINTS =====
                            .requestMatchers(
                                    "/api/guest/**",
                                    "/api/orders/create",
                                    "/api/rounds/active"
                            ).permitAll()

                            // ===== PUBLIC CARGO/ITEMS =====
                            .requestMatchers(
                                    "/api/cargo/**",
                                    "/api/items/**"
                            ).permitAll()

                            // ===== PUBLIC VOLUNTEER ENDPOINTS =====
                            .requestMatchers(
                                    "/api/volunteer/**"
                            ).permitAll()

                            // ===== PUBLIC CLIENT ENDPOINTS =====
                            .requestMatchers(
                                    "/api/client/**"
                            ).permitAll()

                            // ===== PUBLIC ADMIN ENDPOINTS (for development) =====
                            // TODO: In production, change .permitAll() to .hasRole("ADMIN")
                            .requestMatchers(
                                    "/api/admin/**"
                            ).permitAll()  // Change this in production!

                            // ===== HEALTH & MONITORING =====
                            .requestMatchers(
                                    "/health",
                                    "/actuator/**"
                            ).permitAll()

                            // ===== SWAGGER/API DOCUMENTATION =====
                            .requestMatchers(
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/v3/api-docs/**",
                                    "/swagger-resources/**",
                                    "/webjars/**",
                                    "/api-docs/**"
                            ).permitAll()

                            // ===== STATIC RESOURCES =====
                            .requestMatchers(
                                    "/css/**",
                                    "/js/**",
                                    "/images/**",
                                    "/static/**",
                                    "/favicon.ico",
                                    "/error"
                            ).permitAll()

                            // ===== ALL OTHER ENDPOINTS =====
                            .anyRequest().permitAll();  // TEMPORARY: Allow all for debugging

                    logger.info("✓ Authorization rules configured");
                    logger.info("  - TLS endpoints: PUBLIC");
                    logger.info("  - Security/ECDH endpoints: PUBLIC");
                    logger.info("  - Auth endpoints: PUBLIC");
                    logger.info("  - Guest endpoints: PUBLIC");
                    logger.info("  - Cargo/Items endpoints: PUBLIC");
                    logger.info("  - Volunteer endpoints: PUBLIC");
                    logger.info("  - Client endpoints: PUBLIC");
                    logger.info("  - Admin endpoints: PUBLIC (development)");
                    logger.info("  - ALL OTHER endpoints: PUBLIC (TEMPORARY FOR DEBUGGING)");
                })

                // Disable form login (we use custom auth)
                .formLogin(form -> {
                    form.disable();
                    logger.info("✓ Form login disabled (using custom ECDH auth)");
                })

                // Disable HTTP Basic auth (we use custom auth)
                .httpBasic(basic -> {
                    basic.disable();
                    logger.info("✓ HTTP Basic auth disabled (using custom ECDH auth)");
                })

                // Disable logout (we handle it custom)
                .logout(logout -> {
                    logout.disable();
                    logger.info("✓ Default logout disabled (using custom logout)");
                });

        logger.info("╔══════════════════════════════════════════════════════════════╗");
        logger.info("║   Security Configuration Complete - PERMISSIVE DEBUG MODE    ║");
        logger.info("║   ALL ENDPOINTS ARE PUBLIC for debugging 403 errors!         ║");
        logger.info("║   Change .anyRequest().permitAll() to .authenticated()       ║");
        logger.info("║   after confirming 403 errors are fixed!                     ║");
        logger.info("╚══════════════════════════════════════════════════════════════╝");

        return http.build();
    }

    /**
     * CORS Configuration Source
     * Uses the same origins as RequestCorsFilter for consistency
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins from properties
        List<String> origins = Arrays.asList(corsAllowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        logger.info("✓ CORS allowed origins: {}", origins);

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));

        // Allow all headers (match RequestCorsFilter)
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type", "Authorization", "X-Session-ID", "X-Auth-Token",
                "X-Client-ID", "X-Timestamp", "X-Signature", "Admin-Username",
                "Authentication-Status", "X-User-Role", "X-User-Id",
                "X-Requested-With", "Origin", "Accept"
        ));

        // Expose headers (match RequestCorsFilter)
        configuration.setExposedHeaders(Arrays.asList(
                "X-Session-ID", "X-Auth-Token", "Content-Type"
        ));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Max age
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}