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

  private String password;

  private String email;

  private String timestamp;

  private byte[] vaultsalt;

  public Appuser() {
    this.timestamp = System.currentTimeMillis()+"";
    this.vaultsalt = Crypt.getRandomSalt();
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public byte[] getVaultSalt() {
    return vaultsalt;
  }

  public void setVaultSalt(byte[] vaultsalt) {
    this.vaultsalt = vaultsalt;
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