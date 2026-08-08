package com.neto.Storingator.Entities;

import com.neto.Storingator.Utilities.Crypt;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class Appuser {
  @Id
  @GeneratedValue(strategy=GenerationType.UUID)
  private UUID id;

  @Column(unique = true)
  private String username;

  private String password;

  @Column(unique = true)
  private String email;

  private String timestamp;

  private byte[] vaultSalt;

  public Appuser() {
    this.timestamp = System.currentTimeMillis()+"";
    this.vaultSalt = Crypt.getRandomSalt();
  }
}