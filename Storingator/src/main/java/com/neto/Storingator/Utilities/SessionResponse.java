package com.neto.Storingator.Utilities;

public class SessionResponse {
    private String username;
    private String loginkey;

    public SessionResponse() {
    }

    public SessionResponse(String username, String loginkey) {
        this.username = username;
        this.loginkey = loginkey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLoginkey() {
        return loginkey;
    }

    public void setLoginkey(String loginkey) {
        this.loginkey = loginkey;
    }
}