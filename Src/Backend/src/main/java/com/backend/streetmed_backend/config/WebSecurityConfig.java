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
 * Fixed CORS headers for User-Id and User-Role
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001,https://localhost:3000}")
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
                    logger.info("✓ CORS enabled with custom configuration");
                })

                // Disable CSRF for REST API
                .csrf(csrf -> {
                    csrf.disable();
                    logger.info("✓ CSRF disabled for REST API");
                })

                // Stateless session
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    logger.info("✓ Session management: STATELESS");
                })

                // Configure authorization
                .authorizeHttpRequests(auth -> {
                    logger.info("Configuring endpoint authorization...");

                    auth
                            // ===== PUBLIC ENDPOINTS =====
                            .requestMatchers(
                                    "/api/test/**",
                                    "/api/security/**",
                                    "/api/auth/**",
                                    "/api/guest/**",
                                    "/api/cargo/**",
                                    "/api/items/**",
                                    "/api/orders/**",  // Allow orders endpoints
                                    "/api/rounds/**",   // Allow rounds endpoints
                                    "/api/volunteer/**",
                                    "/api/client/**",
                                    "/api/admin/**",
                                    "/api/feedback/**",
                                    "/health",
                                    "/actuator/**",
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**",
                                    "/error"
                            ).permitAll()

                            // Allow all for development
                            .anyRequest().permitAll();

                    logger.info("✓ Authorization rules configured - ALL ENDPOINTS PUBLIC (Development)");
                })

                // Disable form login
                .formLogin(form -> form.disable())

                // Disable HTTP Basic auth
                .httpBasic(basic -> basic.disable())

                // Disable default logout
                .logout(logout -> logout.disable());

        logger.info("╔══════════════════════════════════════════════════════════════╗");
        logger.info("║   Security Configuration Complete - Development Mode         ║");
        logger.info("╚══════════════════════════════════════════════════════════════╝");

        return http.build();
    }

    /**
     * CORS Configuration Source - FIXED to include User-Id and User-Role headers
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins from properties or use defaults
        List<String> origins = Arrays.asList(corsAllowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        logger.info("✓ CORS allowed origins: {}", origins);

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));

        // Allow all headers - INCLUDING User-Id and User-Role which were missing!
        configuration.setAllowedHeaders(Arrays.asList(
                // Standard headers
                "Content-Type",
                "Accept",
                "Origin",
                "Authorization",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",

                // Session/Auth headers
                "X-Session-ID",
                "X-Auth-Token",
                "X-Client-ID",
                "X-Timestamp",
                "X-Signature",

                // Custom application headers
                "Admin-Username",
                "Authentication-Status",
                "User-Id",
                "User-Role",
                "X-User-Id",
                "X-User-Role"
        ));

        logger.info("✓ CORS allowed headers configured (including User-Id and User-Role)");

        // Expose headers
        configuration.setExposedHeaders(Arrays.asList(
                "X-Session-ID",
                "X-Auth-Token",
                "Content-Type",
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"
        ));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Max age for preflight cache
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        logger.info("✓ CORS configuration complete with all required headers");

        return source;
    }
}