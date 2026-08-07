package com.neto.Storingator.Utilities;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NewData {
    @NotNull(message = "Login data is required")
    private TempLoginData tmpLoginData;

    private String passphrase;
}