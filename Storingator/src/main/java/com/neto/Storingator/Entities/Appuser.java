package com.neto.Storingator.Entities;

import com.neto.Storingator.Utilities.Crypt;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Appuser {
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Integer id;

  private String username;

  private String password;

  private String email;

  private String timestamp;

  private byte[] vaultSalt;

  public Appuser() {
    this.timestamp = System.currentTimeMillis()+"";
    this.vaultSalt = Crypt.getRandomSalt();
  }
}