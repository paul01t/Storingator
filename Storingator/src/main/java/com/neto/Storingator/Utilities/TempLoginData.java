package com.neto.Storingator.Utilities;

import lombok.Data;

@Data
public class TempLoginData {
    private String platform;
    private String username;
    private String email;
    private String password;
    private String extrainfo;
    private int seq;
    private Boolean encryptOnDevice;
}