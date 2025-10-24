package com.backend.streetmed_backend.dto.auth;

import java.util.Map;

public class UpdatePasswordRequest {
    private Map<String, String> updateData;
    private String authToken;
    private String sessionId;
    private boolean isSecure;

    public UpdatePasswordRequest(Map<String, String> updateData, String authToken,
                                 String sessionId, boolean isSecure) {
        this.updateData = updateData;
        this.authToken = authToken;
        this.sessionId = sessionId;
        this.isSecure = isSecure;
    }

    public Map<String, String> getUpdateData() {
        return updateData;
    }

    public void setUpdateData(Map<String, String> updateData) {
        this.updateData = updateData;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public void setSecure(boolean secure) {
        isSecure = secure;
    }
}
