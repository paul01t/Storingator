package com.neto.Storingator.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.neto.Storingator.Entities.Activeusers;

import java.util.UUID;

public interface ActiveUsersRepository extends JpaRepository<Activeusers, UUID> {

  Activeusers findByLoginkey(String loginkey);

  Long deleteByUserseq(UUID userseq);
}