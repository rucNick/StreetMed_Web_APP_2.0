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

        // Allow common HTTP methods
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // Allow all headers
        config.addAllowedHeader("*");

        // Important: Allow authentication
        config.setAllowCredentials(true);

        // Allow specific headers needed for your security implementation
        config.addExposedHeader("X-Session-ID");

        // Set the max age for the preflight request cache (in seconds)
        config.setMaxAge(3600L);

        // Apply CORS config to all endpoints including your security endpoints
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}