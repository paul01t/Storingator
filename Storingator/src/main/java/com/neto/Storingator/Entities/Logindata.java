package com.neto.Storingator.Entities;

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
public class Logindata {
  @Id
  @GeneratedValue(strategy=GenerationType.UUID)
  private UUID seq;

  private UUID userSeq;

  private String platform;

  private String username;

  private String email;

  private byte[] password;

  private byte[] nonce;

  private String extraInfo;

  private Boolean encryptOnDevice;
}