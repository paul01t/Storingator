package com.neto.Storingator.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.neto.Storingator.Entities.Logindata;

public interface LoginDataRepository extends JpaRepository<Logindata, Integer> {

  List<Logindata> findByUsername(String username);

  List<Logindata> findByUserseq(int userseq);

  Logindata findBySeq(int seq);
}