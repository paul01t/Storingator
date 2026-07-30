package com.neto.Storingator.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Activeusers {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer seq;

    private Integer userseq;

    private String username;

    private String loginkey;

    private LocalDateTime startdate;

    public Activeusers() {
        startdate = LocalDateTime.now();
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Integer getUserseq() {
        return userseq;
    }

    public void setUserseq(Integer userseq) {
        this.userseq = userseq;
    }

    public LocalDateTime getStartdate() {
        return startdate;
    }

    public void setStartdate(LocalDateTime startdate) {
        this.startdate = startdate;
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