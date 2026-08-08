package com.neto.Storingator.Controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Entities.Appuser;
import com.neto.Storingator.Entities.Note;
import com.neto.Storingator.Exceptions.BadRequestException;
import com.neto.Storingator.Exceptions.UnauthorizedException;
import com.neto.Storingator.Exceptions.VaultOperationFailed;
import com.neto.Storingator.Service.SessionService;
import com.neto.Storingator.Utilities.Crypt;
import com.neto.Storingator.Utilities.NoteData;
import com.neto.Storingator.Utilities.TempNote;
import com.neto.Storingator.Repository.NoteRepository;
import com.neto.Storingator.Repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/Note")
public class NoteController {

    private static final Logger log = LoggerFactory.getLogger(NoteController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private SessionService sessionService;

    @PostMapping("/New")
    public String createNote(@RequestHeader("Authorization") String bearerToken,
                             @Valid @RequestBody NoteData data) {
        log.info("Received new note request");
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);
        TempNote newNote = data.getTmpNote();

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());

        Note note = new Note();
        note.setTitle(newNote.getTitle());
        note.setUserSeq(activeuser.getUserseq());

        storeContent(note, data, dbUser);

        noteRepository.save(note);
        log.info("Note saved successfully");
        return "Note saved";
    }

    @PostMapping("/Update")
    public String updateNote(@RequestHeader("Authorization") String bearerToken,
                             @Valid @RequestBody NoteData data) {
        log.info("Received update note request");
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);
        TempNote newNote = data.getTmpNote();

        Note existing = noteRepository.findBySeq(newNote.getSeq());
        if (existing == null || !existing.getUserSeq().equals(activeuser.getUserseq())) {
            log.warn("Ownership check failed for seq: {} and user: {}", newNote.getSeq(), activeuser.getUsername());
            throw new UnauthorizedException("Not authorized to modify this note");
        }

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());

        existing.setTitle(newNote.getTitle());

        storeContent(existing, data, dbUser);

        noteRepository.save(existing);
        log.info("Note updated successfully");
        return "Note updated";
    }

    @DeleteMapping("/Delete/{seq}")
    public String deleteNote(@RequestHeader("Authorization") String bearerToken,
                             @PathVariable UUID seq) {
        log.info("Received delete note request for seq: {}", seq);
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);

        Note existing = noteRepository.findBySeq(seq);
        if (existing == null || !existing.getUserSeq().equals(activeuser.getUserseq())) {
            log.warn("Ownership check failed for seq: {} and user: {}", seq, activeuser.getUsername());
            throw new UnauthorizedException("Not authorized to delete this note");
        }

        noteRepository.delete(existing);
        log.info("Note deleted for seq: {} and user: {}", seq, activeuser.getUsername());
        return "Note deleted";
    }

    private void storeContent(Note note, NoteData data, Appuser dbUser) {
        TempNote newNote = data.getTmpNote();
        if (newNote.getContent() == null || newNote.getContent().isEmpty()) {
            throw new BadRequestException("Note content is required");
        }

        boolean encryptOnDevice = Boolean.TRUE.equals(newNote.getEncryptOnDevice());
        note.setEncryptOnDevice(encryptOnDevice);

        if (encryptOnDevice) {
            note.setContent(newNote.getContent().getBytes(StandardCharsets.UTF_8));
            note.setNonce(null);
            return;
        }

        if (data.getPassphrase() == null || data.getPassphrase().isEmpty()) {
            throw new BadRequestException("Passphrase is required for server-side encryption");
        }

        try {
            byte[] nonce = Crypt.getRandomNonce();
            byte[] key = Crypt.deriveKeyFromPassphrase(data.getPassphrase(), dbUser.getVaultSalt());
            byte[] contentByte = newNote.getContent().getBytes(StandardCharsets.UTF_8);
            note.setContent(Crypt.encrypt(contentByte, key, nonce));
            note.setNonce(nonce);
        } catch (Exception e) {
            log.error("Failed to encrypt note for user: {}", dbUser.getUsername(), e);
            throw new VaultOperationFailed("Failed to encrypt note");
        }
    }

    @PostMapping("/GetNotes")
    public List<TempNote> GetNotes(@RequestHeader("Authorization") String bearerToken,
                                   @RequestBody(required = false) NoteData data) {
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);
        log.info("Fetching notes for user: {}", activeuser.getUsername());

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());
        List<TempNote> list = new ArrayList<>();
        List<Note> noteList = noteRepository.findByUserSeq(activeuser.getUserseq());

        boolean hasServerEncrypted = noteList.stream()
                .anyMatch(note -> !Boolean.TRUE.equals(note.getEncryptOnDevice()));
        String passphrase = data == null ? null : data.getPassphrase();
        if (hasServerEncrypted && (passphrase == null || passphrase.isEmpty())) {
            throw new BadRequestException("Passphrase is required to decrypt notes");
        }

        byte[] key = null;
        if (hasServerEncrypted) {
            try {
                key = Crypt.deriveKeyFromPassphrase(passphrase, dbUser.getVaultSalt());
            } catch (Exception e) {
                log.error("Failed to derive key for user: {}", activeuser.getUsername(), e);
                throw new VaultOperationFailed("Failed to decrypt notes");
            }
        }

        for (Note note : noteList) {
            TempNote temp = new TempNote();
            temp.setTitle(note.getTitle());
            temp.setSeq(note.getSeq());
            temp.setTimestamp(note.getTimestamp());
            temp.setEncryptOnDevice(note.getEncryptOnDevice());
            if (Boolean.TRUE.equals(note.getEncryptOnDevice())) {
                byte[] content = note.getContent();
                temp.setContent(content == null ? "" : new String(content, StandardCharsets.UTF_8));
            } else {
                try {
                    temp.setContent(Crypt.decrypt(note.getContent(), key, note.getNonce()));
                } catch (Exception e) {
                    log.error("Failed to decrypt note for seq: {} and user: {}", note.getSeq(), activeuser.getUsername(), e);
                    throw new BadRequestException("Could not decrypt notes. Check your passphrase.");
                }
            }
            list.add(temp);
        }

        log.info("Returned {} notes", list.size());
        return list;
    }

    @PostMapping("/GetNote")
    public TempNote GetNote(@RequestHeader("Authorization") String bearerToken,
                            @Valid @RequestBody NoteData data) {
        log.info("Fetching note for seq: {}", data.getTmpNote().getSeq());
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);

        TempNote temp = data.getTmpNote();
        Note note = noteRepository.findBySeq(data.getTmpNote().getSeq());

        if (note == null || !note.getUserSeq().equals(activeuser.getUserseq())) {
            log.warn("Ownership check failed for seq: {} and user: {}", data.getTmpNote().getSeq(), activeuser.getUsername());
            throw new UnauthorizedException("Not authorized to access this note");
        }

        temp.setTitle(note.getTitle());
        temp.setTimestamp(note.getTimestamp());

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());
        if (Boolean.TRUE.equals(note.getEncryptOnDevice())) {
            byte[] content = note.getContent();
            temp.setContent(content == null ? "" : new String(content, StandardCharsets.UTF_8));
            return temp;
        }
        if (data.getPassphrase() == null || data.getPassphrase().isEmpty()) {
            throw new BadRequestException("Passphrase is required to decrypt note");
        }
        try {
            byte[] key = Crypt.deriveKeyFromPassphrase(data.getPassphrase(), dbUser.getVaultSalt());
            temp.setContent(Crypt.decrypt(note.getContent(), key, note.getNonce()));
        } catch (Exception e) {
            log.error("Failed to decrypt note for seq: {}", data.getTmpNote().getSeq(), e);
            throw new BadRequestException("Could not decrypt note. Check your passphrase.");
        }

        return temp;
    }
}