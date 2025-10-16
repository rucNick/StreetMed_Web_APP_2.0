package com.backend.streetmed_backend.dto.order;

public class GetPendingOrdersRequest {
    private String authStatus;
    private Integer userId;
    private String userRole;
    private Integer page;
    private Integer size;

    public GetPendingOrdersRequest(String authStatus, Integer userId, String userRole, Integer page, Integer size) {
        this.authStatus = authStatus;
        this.userId = userId;
        this.userRole = userRole;
        this.page = page != null ? page : 0;
        this.size = size != null ? size : 20;
    }

    public String getAuthStatus() { return authStatus; }
    public void setAuthStatus(String authStatus) { this.authStatus = authStatus; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}