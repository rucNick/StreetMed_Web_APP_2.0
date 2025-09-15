package com.backend.streetmed_backend.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OptionsRequestFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(OptionsRequestFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        // Always respond to OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // Allow requested origin
            String origin = request.getHeader("Origin");

            logger.debug("Processing OPTIONS request from origin: {}, path: {}",
                    origin, request.getRequestURI());

            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");

            // Include ALL custom headers including X-Auth-Token
            response.setHeader("Access-Control-Allow-Headers",
                    "Content-Type, Authorization, X-Session-ID, X-Auth-Token, X-Client-ID, X-Timestamp, X-Signature, " +
                            "Admin-Username, Authentication-Status, X-User-Role, X-User-Id, X-Requested-With, Origin, Accept, " +
                            "Access-Control-Request-Method, Access-Control-Request-Headers");

            response.setHeader("Access-Control-Expose-Headers", "X-Session-ID, X-Auth-Token");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setStatus(HttpServletResponse.SC_OK);

            logger.debug("OPTIONS request handled successfully for path: {}", request.getRequestURI());
            return;
        }

        chain.doFilter(req, res);
    }
}