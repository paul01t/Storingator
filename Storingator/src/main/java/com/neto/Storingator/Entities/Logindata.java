package com.neto.Storingator.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Logindata {
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Integer seq;

  private Integer userSeq;

  private String platform;

  private String username;

  private String email;

  private byte[] password;

  private byte[] nonce;

  private String extraInfo;
}