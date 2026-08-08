package com.neto.Storingator.Utilities;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class TempLoginData {
    @Size(max = 128, message = "Platform must not exceed 128 characters")
    private String platform;

    @Size(max = 256, message = "Username must not exceed 256 characters")
    private String username;

    @Email(message = "Email must be a valid address")
    @Size(max = 128, message = "Email must not exceed 128 characters")
    private String email;

    @Size(max = 4096, message = "Password must not exceed 4096 characters")
    private String password;

    @Size(max = 4096, message = "Extra info must not exceed 4096 characters")
    private String extrainfo;

    private UUID seq;
    private Boolean encryptOnDevice;
}