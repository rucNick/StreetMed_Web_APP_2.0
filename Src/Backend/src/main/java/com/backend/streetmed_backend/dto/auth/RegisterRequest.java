package com.backend.streetmed_backend.dto.auth;

import java.util.Map;

public class RegisterRequest {
    private Map<String, String> userData;
    private String sessionId;
    private boolean isSecure;

    public RegisterRequest(Map<String, String> userData, String sessionId, boolean isSecure) {
        this.userData = userData;
        this.sessionId = sessionId;
        this.isSecure = isSecure;
    }

    // Getters and setters
    public Map<String, String> getUserData() { return userData; }
    public void setUserData(Map<String, String> userData) { this.userData = userData; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public boolean isSecure() { return isSecure; }
    public void setSecure(boolean secure) { isSecure = secure; }
}
