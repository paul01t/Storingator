package com.neto.Storingator.Utilities;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteUserRequest {
    @NotBlank(message = "Password is required")
    private String password;
}