package com.neto.Storingator.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.neto.Storingator.Entities.Activeusers;

public interface ActiveUsersRepository extends JpaRepository<Activeusers, Long> {

  Activeusers getActiveUsersByUsername(String username);

  Long deleteByUserseq(int userseq);
}