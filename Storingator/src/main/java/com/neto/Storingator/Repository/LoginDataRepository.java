package com.neto.Storingator.Repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.neto.Storingator.Entities.Logindata;

public interface LoginDataRepository extends JpaRepository<Logindata, UUID> {

  List<Logindata> findByUsername(String username);

  List<Logindata> findByUserSeq(UUID userSeq);

  Logindata findBySeq(UUID seq);

  Long deleteByUserSeq(UUID userSeq);
}