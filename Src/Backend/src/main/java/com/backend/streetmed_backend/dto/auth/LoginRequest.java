package com.backend.streetmed_backend.dto.auth;

import java.util.Map;

public class LoginRequest {
    private Map<String, String> credentials;
    private String sessionId;
    private boolean isSecure;

    public LoginRequest(Map<String, String> credentials, String sessionId, boolean isSecure) {
        this.credentials = credentials;
        this.sessionId = sessionId;
        this.isSecure = isSecure;
    }

    // Getters and setters
    public Map<String, String> getCredentials() { return credentials; }
    public void setCredentials(Map<String, String> credentials) { this.credentials = credentials; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public boolean isSecure() { return isSecure; }
    public void setSecure(boolean secure) { isSecure = secure; }
}
