package com.neto.Storingator.Entities;

import com.neto.Storingator.Utilities.Crypt;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Appuser {
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Integer seq;

  private String username;

  private byte[] password;

  private String email;

  private byte[] nonce;

  private String pswkey;

  private String timestamp;

  public Appuser() {
    this.timestamp = System.currentTimeMillis()+"";
    String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?";
    String pwd = Crypt.randomPassphrase(50, characters);
    this.pswkey = pwd;
  }

  public Integer getId() {
    return seq;
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

  public String getPswKey() {
    return pswkey;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public void setPswkey(String pswkey) {
    this.pswkey = pswkey;
  }

  @Override
  public String toString() {
    return "User{" +
            "seq=" + seq +
            ", username='" + username + '\'' +
            ", email='" + email + '\'' +
            ", timestamp='" + timestamp + '\'' +
            '}';
  }
}