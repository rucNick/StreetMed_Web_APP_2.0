package com.backend.streetmed_backend.controller.Test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * TLS Test Controller - PUBLIC ENDPOINTS (NO AUTHENTICATION REQUIRED)
 * Used for certificate validation and TLS verification
 */
@Tag(name = "TLS Test", description = "Public TLS/SSL verification endpoints")
@RestController
@RequestMapping("/api/test/tls")
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000", "http://127.0.0.1:3000", "https://127.0.0.1:3000"},
        allowCredentials = "true",
        allowedHeaders = "*")
public class TLSTestController {

    private static final Logger logger = LoggerFactory.getLogger(TLSTestController.class);

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.port:8443}")
    private int httpsPort;

    @Value("${server.http.port:8080}")
    private int httpPort;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * PUBLIC ENDPOINT - TLS Status Check (JSON)
     * Returns JSON with TLS configuration and connection details
     * NO AUTHENTICATION REQUIRED
     */
    @Operation(summary = "Check TLS Status (JSON)",
            description = "Returns current connection security status as JSON")
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> checkTLSStatus(HttpServletRequest request) {
        logger.info("TLS status check from: {} ({})", request.getRemoteAddr(), request.getHeader("User-Agent"));

        Map<String, Object> response = new HashMap<>();

        // Basic info
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "TLS Test Endpoint - Connection Successful!");
        response.put("status", "success");

        // Server configuration
        Map<String, Object> serverConfig = new HashMap<>();
        serverConfig.put("sslEnabled", sslEnabled);
        serverConfig.put("httpsPort", httpsPort);
        serverConfig.put("httpPort", httpPort);
        serverConfig.put("activeProfile", activeProfile);
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

        // Check for proxy headers
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

        Map<String, Object> tlsStatus = new HashMap<>();
        tlsStatus.put("usingTLS", isUsingTLS);
        tlsStatus.put("status", isUsingTLS ? "SECURED" : "NOT_SECURED");
        tlsStatus.put("recommendation", isUsingTLS ?
                "✓ Connection is secured with TLS/SSL" :
                "⚠ Connection is NOT secured - using plain HTTP");
        response.put("tlsStatus", tlsStatus);

        logger.info("TLS Status: Scheme={}, Secure={}, Port={}, Using TLS={}",
                request.getScheme(), request.isSecure(), request.getServerPort(), isUsingTLS);

        return ResponseEntity.ok(response);
    }

    /**
     * PUBLIC ENDPOINT - Certificate Acceptance Page (HTML)
     * Shows a nice HTML page confirming certificate acceptance
     * This is the page users see when accepting the self-signed cert
     * NO AUTHENTICATION REQUIRED
     */
    @Operation(summary = "Certificate Test Page (HTML)",
            description = "Shows HTML page for certificate acceptance confirmation")
    @GetMapping(value = "/cert-test", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> certTestPage(HttpServletRequest request) {
        logger.info("Certificate test page accessed from: {} via {}",
                request.getRemoteAddr(), request.getScheme().toUpperCase());

        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getScheme());

        String html = String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Certificate Accepted - StreetMed Backend</title>
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }
                        
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            min-height: 100vh;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            padding: 20px;
                        }
                        
                        .container {
                            background: white;
                            padding: 50px;
                            border-radius: 20px;
                            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                            max-width: 600px;
                            width: 100%%;
                            text-align: center;
                            animation: slideUp 0.5s ease-out;
                        }
                        
                        @keyframes slideUp {
                            from {
                                opacity: 0;
                                transform: translateY(30px);
                            }
                            to {
                                opacity: 1;
                                transform: translateY(0);
                            }
                        }
                        
                        .success-icon {
                            width: 100px;
                            height: 100px;
                            background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%);
                            border-radius: 50%%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            margin: 0 auto 30px;
                            animation: checkmark 0.5s ease-in-out 0.3s both;
                        }
                        
                        @keyframes checkmark {
                            0%% {
                                transform: scale(0);
                            }
                            50%% {
                                transform: scale(1.2);
                            }
                            100%% {
                                transform: scale(1);
                            }
                        }
                        
                        .success-icon svg {
                            width: 60px;
                            height: 60px;
                            fill: white;
                        }
                        
                        h1 {
                            color: #4CAF50;
                            font-size: 32px;
                            margin-bottom: 15px;
                            font-weight: 700;
                        }
                        
                        .subtitle {
                            color: #666;
                            font-size: 18px;
                            margin-bottom: 30px;
                            line-height: 1.6;
                        }
                        
                        .info-box {
                            background: #f8f9fa;
                            padding: 20px;
                            border-radius: 10px;
                            margin: 25px 0;
                            text-align: left;
                        }
                        
                        .info-item {
                            display: flex;
                            justify-content: space-between;
                            padding: 10px 0;
                            border-bottom: 1px solid #e0e0e0;
                        }
                        
                        .info-item:last-child {
                            border-bottom: none;
                        }
                        
                        .info-label {
                            font-weight: 600;
                            color: #333;
                        }
                        
                        .info-value {
                            color: #666;
                            font-family: 'Courier New', monospace;
                        }
                        
                        .secure-badge {
                            background: #4CAF50;
                            color: white;
                            padding: 4px 12px;
                            border-radius: 15px;
                            font-size: 14px;
                            font-weight: 600;
                        }
                        
                        .insecure-badge {
                            background: #ff9800;
                            color: white;
                            padding: 4px 12px;
                            border-radius: 15px;
                            font-size: 14px;
                            font-weight: 600;
                        }
                        
                        .instructions {
                            background: #e3f2fd;
                            padding: 20px;
                            border-radius: 10px;
                            margin-top: 25px;
                            border-left: 4px solid #2196F3;
                        }
                        
                        .instructions h3 {
                            color: #1976D2;
                            margin-bottom: 15px;
                            font-size: 18px;
                        }
                        
                        .instructions p {
                            color: #555;
                            line-height: 1.8;
                            margin: 10px 0;
                        }
                        
                        .close-btn {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            border: none;
                            padding: 15px 40px;
                            border-radius: 30px;
                            font-size: 16px;
                            font-weight: 600;
                            cursor: pointer;
                            margin-top: 30px;
                            transition: all 0.3s ease;
                            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
                        }
                        
                        .close-btn:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
                        }
                        
                        .close-btn:active {
                            transform: translateY(0);
                        }
                        
                        .auto-close {
                            color: #999;
                            font-size: 14px;
                            margin-top: 15px;
                        }
                        
                        @media (max-width: 600px) {
                            .container {
                                padding: 30px 20px;
                            }
                            
                            h1 {
                                font-size: 24px;
                            }
                            
                            .subtitle {
                                font-size: 16px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="success-icon">
                            <svg viewBox="0 0 24 24">
                                <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/>
                            </svg>
                        </div>
                        
                        <h1>Certificate Accepted!</h1>
                        <p class="subtitle">
                            Your browser has successfully accepted the self-signed SSL certificate for local development.
                        </p>
                        
                        <div class="info-box">
                            <div class="info-item">
                                <span class="info-label">SSL Enabled:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">HTTPS Port:</span>
                                <span class="info-value">%d</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">HTTP Port:</span>
                                <span class="info-value">%d</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Connection:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Protocol:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Status:</span>
                                <span class="%s">%s</span>
                            </div>
                        </div>
                        
                        <div class="instructions">
                            <h3>✓ What's Next?</h3>
                            <p>
                                <strong>1.</strong> Close this tab or click the button below
                            </p>
                            <p>
                                <strong>2.</strong> Return to your application and click "Check Connection"
                            </p>
                            <p>
                                <strong>3.</strong> Your application should now work with HTTPS!
                            </p>
                        </div>
                        
                        <button class="close-btn" onclick="window.close()">
                            Close This Tab
                        </button>
                        
                        <p class="auto-close">
                            This tab will automatically close in <span id="countdown">5</span> seconds
                        </p>
                    </div>
                    
                    <script>
                        // Countdown timer
                        let seconds = 5;
                        const countdownEl = document.getElementById('countdown');
                        
                        const countdown = setInterval(() => {
                            seconds--;
                            countdownEl.textContent = seconds;
                            
                            if (seconds <= 0) {
                                clearInterval(countdown);
                                window.close();
                            }
                        }, 1000);
                        
                        // If window.close() doesn't work (some browsers prevent it),
                        // at least show a message
                        setTimeout(() => {
                            if (!window.closed) {
                                document.querySelector('.auto-close').innerHTML = 
                                    '<strong>You can safely close this tab now</strong>';
                            }
                        }, 5500);
                    </script>
                </body>
                </html>
                """,
                sslEnabled ? "Yes" : "No",
                httpsPort,
                httpPort,
                isSecure ? "HTTPS (Secure)" : "HTTP",
                request.getProtocol(),
                isSecure ? "secure-badge" : "insecure-badge",
                isSecure ? "✓ Secure" : "⚠ Not Secure"
        );

        logger.info("Certificate test page served successfully - Secure: {}", isSecure);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    /**
     * PUBLIC ENDPOINT - Simple Health Check
     * NO AUTHENTICATION REQUIRED
     */
    @Operation(summary = "Health Check",
            description = "Simple health check endpoint")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "StreetMed Backend");
        response.put("message", "Server is running");

        logger.debug("Health check successful");
        return ResponseEntity.ok(response);
    }
}