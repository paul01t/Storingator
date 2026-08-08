package com.neto.Storingator.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.neto.Storingator.Entities.Appuser;

import java.util.UUID;

public interface UserRepository extends JpaRepository<Appuser, UUID> {

  Appuser findByUsername(String username);

  Appuser findByEmail(String email);
}