package com.neto.Storingator.Utilities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NewData {
    @NotNull(message = "Login data is required")
    @Valid
    private TempLoginData tmpLoginData;

    private String passphrase;
}