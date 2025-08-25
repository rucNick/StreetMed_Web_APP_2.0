package com.backend.streetmed_backend.controller.Test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple test controller to verify TLS configuration
 * Access this endpoint to check if TLS is working properly
 */
@Tag(name = "TLS Test", description = "Test endpoint for TLS/SSL verification")
@RestController
@RequestMapping("/api/test/tls")
public class TLSTestController {

    private static final Logger logger = LoggerFactory.getLogger(TLSTestController.class);

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.port:8443}")
    private int httpsPort;

    @Value("${server.http.port:8080}")
    private int httpPort;

    /**
     * Simple test endpoint to verify TLS status
     * Try accessing:
     * - http://localhost:8080/api/test/tls/status (HTTP)
     * - https://localhost:8443/api/test/tls/status (HTTPS)
     */
    @Operation(summary = "Check TLS Status",
            description = "Returns current connection security status")
    @GetMapping("/status")
    public Map<String, Object> checkTLSStatus(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Basic info
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "TLS Test Endpoint");

        // Server configuration
        Map<String, Object> serverConfig = new HashMap<>();
        serverConfig.put("sslEnabled", sslEnabled);
        serverConfig.put("httpsPort", httpsPort);
        serverConfig.put("httpPort", httpPort);
        response.put("serverConfig", serverConfig);

        // Current connection info
        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("scheme", request.getScheme());
        connectionInfo.put("secure", request.isSecure());
        connectionInfo.put("protocol", request.getProtocol());
        connectionInfo.put("serverName", request.getServerName());
        connectionInfo.put("serverPort", request.getServerPort());
        connectionInfo.put("requestURL", request.getRequestURL().toString());
        response.put("connection", connectionInfo);

        // Check for proxy headers (useful when behind load balancer)
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        if (xForwardedProto != null) {
            Map<String, String> proxyInfo = new HashMap<>();
            proxyInfo.put("X-Forwarded-Proto", xForwardedProto);
            proxyInfo.put("X-Forwarded-Port", request.getHeader("X-Forwarded-Port"));
            proxyInfo.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
            response.put("proxyHeaders", proxyInfo);
        }

        // Determine TLS status
        boolean isUsingTLS = request.isSecure() ||
                "https".equalsIgnoreCase(request.getScheme()) ||
                "https".equalsIgnoreCase(xForwardedProto);

        // Status summary
        Map<String, Object> status = new HashMap<>();
        status.put("usingTLS", isUsingTLS);
        status.put("status", isUsingTLS ? "SECURED" : "NOT_SECURED");
        status.put("recommendation", isUsingTLS ?
                "✓ Connection is secured with TLS/SSL" :
                "⚠ Connection is NOT secured - using plain HTTP");
        response.put("tlsStatus", status);

        // Log the access
        logger.info("TLS Status Check - Scheme: {}, Secure: {}, Port: {}, Using TLS: {}",
                request.getScheme(), request.isSecure(), request.getServerPort(), isUsingTLS);

        return response;
    }
}