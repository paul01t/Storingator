package com.neto.Storingator.Repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.neto.Storingator.Entities.Note;

public interface NoteRepository extends JpaRepository<Note, UUID> {

  List<Note> findByUserSeq(UUID userSeq);

  Note findBySeq(UUID seq);

  Long deleteByUserSeq(UUID userSeq);
}