package com.backend.streetmed_backend.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TLS Configuration for both local development and production
 * Enable by setting server.ssl.enabled=true in application properties
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class TLSConfig {

    private static final Logger logger = LoggerFactory.getLogger(TLSConfig.class);

    @Value("${server.http.port:8080}")
    private int httpPort;

    @Value("${server.port:8443}")
    private int httpsPort;

    @Value("${tls.force.https:false}")
    private boolean forceHttps;

    @Value("${tls.dual.port.enabled:true}")
    private boolean dualPortEnabled;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * Configure embedded Tomcat for TLS support
     * In development: allows both HTTP and HTTPS
     * In production: force HTTPS only
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        logger.info("Initializing TLS configuration for profile: {}", activeProfile);
        logger.info("HTTPS Port: {}, HTTP Port: {}, Force HTTPS: {}, Dual Port: {}",
                httpsPort, httpPort, forceHttps, dualPortEnabled);

        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                // Only force HTTPS if explicitly configured
                if (forceHttps) {
                    logger.info("Forcing HTTPS for all connections");
                    SecurityConstraint securityConstraint = new SecurityConstraint();
                    securityConstraint.setUserConstraint("CONFIDENTIAL");
                    SecurityCollection collection = new SecurityCollection();
                    collection.addPattern("/*");
                    securityConstraint.addCollection(collection);
                    context.addConstraint(securityConstraint);
                }
            }
        };

        // Add HTTP connector for dual-port mode
        if (dualPortEnabled) {
            Connector httpConnector = createHttpConnector();
            tomcat.addAdditionalTomcatConnectors(httpConnector);
            logger.info("Dual-port mode enabled: HTTP on {} and HTTPS on {}", httpPort, httpsPort);
        } else {
            logger.info("Single-port mode: HTTPS only on port {}", httpsPort);
        }

        return tomcat;
    }

    /**
     * Create HTTP connector
     * In development: allows plain HTTP
     * In production with forceHttps: redirects to HTTPS
     */
    private Connector createHttpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);

        if (forceHttps) {
            connector.setRedirectPort(httpsPort);
            logger.info("HTTP connector will redirect to HTTPS port {}", httpsPort);
        } else {
            logger.info("HTTP connector accepting plain HTTP on port {}", httpPort);
        }

        // Relax query parameters for development
        if (isLocalProfile()) {
            connector.setProperty("relaxedQueryChars", "[]|{}^&#x5c;&#x60;&quot;&lt;&gt;");
            connector.setProperty("relaxedPathChars", "[]|");
        }

        return connector;
    }

    /**
     * Check if running in local/dev profile
     */
    private boolean isLocalProfile() {
        return activeProfile.contains("local") ||
                activeProfile.contains("dev") ||
                activeProfile.contains("default");
    }
}