package com.neto.Storingator.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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
}