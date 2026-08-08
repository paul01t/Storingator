package com.neto.Storingator.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Activeusers {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID seq;

    private UUID userseq;

    private String username;

    @Column(unique = true)
    private String loginkey;

    private LocalDateTime startdate;

    public Activeusers() {
        startdate = LocalDateTime.now();
    }
}