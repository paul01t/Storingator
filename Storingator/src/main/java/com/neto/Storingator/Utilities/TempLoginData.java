package com.neto.Storingator.Utilities;

public class TempLoginData {
    private String platform;
    private String username;
    private String email;
    private String password;
    private String extrainfo;
    private int seq;

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExtrainfo() {
        return extrainfo;
    }

    public void setExtrainfo(String extraInfo) {
        this.extrainfo = extraInfo;
    }
}