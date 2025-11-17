package com.backend.streetmed_backend.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating standardized API responses
 * Eliminates redundancy across all controllers
 */
public class ResponseUtil {

    /**
     * Create HTTPS required error response
     */
    public static ResponseEntity<Map<String, Object>> httpsRequired() {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "This operation requires secure HTTPS connection");
        errorResponse.put("httpsRequired", true);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Create HTTPS required error response with custom message
     */
    public static ResponseEntity<Map<String, Object>> httpsRequired(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        errorResponse.put("httpsRequired", true);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Create generic error response
     */
    public static ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        errorResponse.put("authenticated", false);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Create error response with authentication status
     */
    public static ResponseEntity<Map<String, Object>> error(String message, HttpStatus status, boolean authenticated) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        errorResponse.put("authenticated", authenticated);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Create unauthorized error response
     */
    public static ResponseEntity<Map<String, Object>> unauthorized() {
        return error("Not authenticated", HttpStatus.UNAUTHORIZED);
    }

    /**
     * Create unauthorized error response with custom message
     */
    public static ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return error(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Create success response with message only
     */
    public static ResponseEntity<Map<String, Object>> success(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("authenticated", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Create success response with message and data
     */
    public static ResponseEntity<Map<String, Object>> success(String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("authenticated", true);
        if (data != null) {
            response.putAll(data);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Create success response with data only (no message)
     */
    public static ResponseEntity<Map<String, Object>> successData(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("authenticated", true);
        if (data != null) {
            response.putAll(data);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Create bad request error response
     */
    public static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return error(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Create not found error response
     */
    public static ResponseEntity<Map<String, Object>> notFound(String message) {
        return error(message, HttpStatus.NOT_FOUND);
    }

    /**
     * Create conflict error response
     */
    public static ResponseEntity<Map<String, Object>> conflict(String message) {
        return error(message, HttpStatus.CONFLICT);
    }

    /**
     * Create forbidden error response
     */
    public static ResponseEntity<Map<String, Object>> forbidden(String message) {
        return error(message, HttpStatus.FORBIDDEN);
    }

    /**
     * Create internal server error response
     */
    public static ResponseEntity<Map<String, Object>> internalError(String message) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Add additional data to an existing response
     */
    public static ResponseEntity<Map<String, Object>> withData(ResponseEntity<Map<String, Object>> response,
                                                               String key, Object value) {
        Map<String, Object> body = new HashMap<>(response.getBody());
        body.put(key, value);
        return ResponseEntity.status(response.getStatusCode()).body(body);
    }

    /**
     * Add secure connection status to response
     */
    public static ResponseEntity<Map<String, Object>> withSecureStatus(ResponseEntity<Map<String, Object>> response,
                                                                       boolean isSecure) {
        return withData(response, "secure", isSecure);
    }
}