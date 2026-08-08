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
public class Note {
  @Id
  @GeneratedValue(strategy=GenerationType.UUID)
  private UUID seq;

  private UUID userSeq;

  private String title;

  private byte[] content;

  private byte[] nonce;

  private Boolean encryptOnDevice;

  private String timestamp;

  public Note() {
    this.timestamp = System.currentTimeMillis()+"";
  }
}