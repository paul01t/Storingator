package com.neto.Storingator.Utilities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NewData {
    @NotNull(message = "Login data is required")
    private TempLoginData tmpLoginData;

    @NotBlank(message = "Passphrase is required")
    private String passphrase;

    public TempLoginData getTmpLoginData() {
        return tmpLoginData;
    }

    public void setTmpLoginData(TempLoginData tmpLoginData) {
        this.tmpLoginData = tmpLoginData;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}