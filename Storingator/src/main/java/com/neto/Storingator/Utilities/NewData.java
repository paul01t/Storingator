package com.neto.Storingator.Utilities;

import com.neto.Storingator.Entities.Activeusers;

public class NewData {
    private Activeusers ac;
    private TempLoginData tmpLoginData;
    private String passphrase;

    public Activeusers getAc() {
        return ac;
    }

    public void setAc(Activeusers ac) {
        this.ac = ac;
    }

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