package com.neto.Storingator.Utilities;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TempUser {
    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must not exceed 72 characters")
    private String clearPassword;

    @NotBlank(message = "Username is required")
    @Size(max = 64, message = "Username must not exceed 64 characters")
    private String username;

    @Email(message = "Email must be a valid address")
    @Size(max = 128, message = "Email must not exceed 128 characters")
    private String email;

    public String getClearPassword() {
        return clearPassword;
    }

    public void setClearPassword(String clearPassword) {
        this.clearPassword = clearPassword;
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
}