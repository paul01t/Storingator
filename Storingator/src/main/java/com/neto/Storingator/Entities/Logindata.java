package com.neto.Storingator.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Arrays;

@Entity
public class Logindata {
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Integer seq;

  private Integer userseq;

  private String platform;

  private String username;

  private String email;

  private byte[] password;

  private byte[] nonce;

  private String extrainfo;

  public Integer getSeq() {
    return seq;
  }

  public void setSeq(Integer seq) {
    this.seq = seq;
  }

  public Integer getUserSeq() {
    return userseq;
  }

  public void setUserSeq(Integer userSeq) {
    this.userseq = userSeq;
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

  public byte[] getPassword() {
    return password;
  }

  public void setPassword(byte[] password) {
    this.password = password;
  }

  public byte[] getNonce() {
    return nonce;
  }

  public void setNonce(byte[] nonce) {
    this.nonce = nonce;
  }

  public String getExtraInfo() {
    return extrainfo;
  }

  public void setExtraInfo(String extrainfo) {
    this.extrainfo = extrainfo;
  }

  @Override
  public String toString() {
    return "LoginData [seq=" + seq + ", userSeq=" + userseq + ", platform=" + platform + ", username=" + username
            + ", email=" + email + ", password=" + Arrays.toString(password) + ", nonce=" + Arrays.toString(nonce)
            + ", extraInfo=" + extrainfo + "]";
  }
}