package com.neto.Storingator.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.neto.Storingator.Entities.Appuser;

public interface UserRepository extends JpaRepository<Appuser, Long> {

  Appuser findByUsername(String username);

  Appuser findByEmail(String email);
}