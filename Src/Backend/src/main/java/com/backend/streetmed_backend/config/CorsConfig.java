package com.backend.streetmed_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Log the configured allowed origins for debugging
        logger.info("Configured CORS allowed origins: {}", allowedOrigins);

        // Split and add allowed origins
        String[] origins = allowedOrigins.split(",");
        for (String origin : origins) {
            origin = origin.trim();
            logger.info("Adding allowed origin: {}", origin);
            config.addAllowedOrigin(origin);
        }

        // Allow all common HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));

        // Allow all headers including X-Auth-Token
        config.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Session-ID",
                "X-Auth-Token",
                "X-Client-ID",
                "X-Timestamp",
                "X-Signature",
                "X-User-Role",
                "X-User-Id",
                "Admin-Username",
                "Authentication-Status",
                "X-Requested-With",
                "Origin",
                "Accept",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Important: Allow authentication
        config.setAllowCredentials(true);

        // Expose headers needed for your security implementation
        config.setExposedHeaders(Arrays.asList("X-Session-ID", "X-Auth-Token", "Authorization"));

        // Set the max age for the preflight request cache (in seconds)
        config.setMaxAge(3600L);

        // Apply CORS config to all endpoints including your security endpoints
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}