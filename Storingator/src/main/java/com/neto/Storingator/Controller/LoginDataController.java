package com.neto.Storingator.Controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Entities.Appuser;
import com.neto.Storingator.Entities.Logindata;
import com.neto.Storingator.Exceptions.BadRequestException;
import com.neto.Storingator.Exceptions.UnauthorizedException;
import com.neto.Storingator.Exceptions.VaultOperationFailed;
import com.neto.Storingator.Service.SessionService;
import com.neto.Storingator.Utilities.Crypt;
import com.neto.Storingator.Utilities.NewData;
import com.neto.Storingator.Utilities.TempLoginData;
import com.neto.Storingator.Repository.LoginDataRepository;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginDataController {

    private static final Logger log = LoggerFactory.getLogger(LoginDataController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoginDataRepository logDataRepository;
    @Autowired
    private SessionService sessionService;

    @PostMapping("/Data/New")
    public String createLoginData(@RequestHeader("Authorization") String bearerToken,
                                  @Valid @RequestBody NewData data) {
        log.info("Received new login data request");
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);
        TempLoginData newLoginData = data.getTmpLoginData();

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());

        Logindata loginData = new Logindata();
        loginData.setUsername(newLoginData.getUsername());
        loginData.setEmail(newLoginData.getEmail());
        loginData.setExtraInfo(newLoginData.getExtrainfo());
        loginData.setPlatform(newLoginData.getPlatform());
        loginData.setUserSeq(activeuser.getUserseq());

        storePassword(loginData, data, dbUser);

        logDataRepository.save(loginData);
        log.info("Login data saved successfully");
        return "Login data saved";
    }

    @PostMapping("/Data/Update")
    public String updateLoginData(@RequestHeader("Authorization") String bearerToken,
                                  @Valid @RequestBody NewData data) {
        log.info("Received update login data request");
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);
        TempLoginData newLoginData = data.getTmpLoginData();

        Logindata existing = logDataRepository.findBySeq(newLoginData.getSeq());
        if (existing == null || !existing.getUserSeq().equals(activeuser.getUserseq())) {
            log.warn("Ownership check failed for seq: {} and user: {}", newLoginData.getSeq(), activeuser.getUsername());
            throw new UnauthorizedException("Not authorized to modify this login data");
        }

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());

        existing.setUsername(newLoginData.getUsername());
        existing.setEmail(newLoginData.getEmail());
        existing.setExtraInfo(newLoginData.getExtrainfo());
        existing.setPlatform(newLoginData.getPlatform());

        storePassword(existing, data, dbUser);

        logDataRepository.save(existing);
        log.info("Login data updated successfully");
        return "Login data updated";
    }

    @DeleteMapping("/Data/Delete/{seq}")
    public String deleteData(@RequestHeader("Authorization") String bearerToken,
                             @PathVariable int seq) {
        log.info("Received delete login data request for seq: {}", seq);
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);

        Logindata existing = logDataRepository.findBySeq(seq);
        if (existing == null || !existing.getUserSeq().equals(activeuser.getUserseq())) {
            log.warn("Ownership check failed for seq: {} and user: {}", seq, activeuser.getUsername());
            throw new UnauthorizedException("Not authorized to delete this login data");
        }

        logDataRepository.delete(existing);
        log.info("Login data deleted for seq: {} and user: {}", seq, activeuser.getUsername());
        return "Login data deleted";
    }

    private void storePassword(Logindata loginData, NewData data, Appuser dbUser) {
        TempLoginData newLoginData = data.getTmpLoginData();
        if (newLoginData.getPassword() == null || newLoginData.getPassword().isEmpty()) {
            throw new BadRequestException("Password is required");
        }

        boolean encryptOnDevice = Boolean.TRUE.equals(newLoginData.getEncryptOnDevice());
        loginData.setEncryptOnDevice(encryptOnDevice);

        if (encryptOnDevice) {
            loginData.setPassword(newLoginData.getPassword().getBytes(StandardCharsets.UTF_8));
            loginData.setNonce(null);
            return;
        }

        if (data.getPassphrase() == null || data.getPassphrase().isEmpty()) {
            throw new BadRequestException("Passphrase is required for server-side encryption");
        }

        try {
            byte[] nonce = Crypt.getRandomNonce();
            byte[] key = Crypt.deriveKeyFromPassphrase(data.getPassphrase(), dbUser.getVaultSalt());
            byte[] passwordByte = newLoginData.getPassword().getBytes(StandardCharsets.UTF_8);
            loginData.setPassword(Crypt.encrypt(passwordByte, key, nonce));
            loginData.setNonce(nonce);
        } catch (Exception e) {
            log.error("Failed to encrypt login data for user: {}", dbUser.getUsername(), e);
            throw new VaultOperationFailed("Failed to encrypt login data");
        }
    }

    @PostMapping("/Data/GetLoginData")
    public List<TempLoginData> GetLoginData(@RequestHeader("Authorization") String bearerToken) {
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);
        log.info("Fetching login data for user: {}", activeuser.getUsername());
        List<TempLoginData> list = new ArrayList<>();
        List<Logindata> ldList = logDataRepository.findByUserSeq(activeuser.getUserseq());

        for (int i = 0; i < ldList.size(); i++) {
            TempLoginData temp = new TempLoginData();
            temp.setEmail(ldList.get(i).getEmail());
            temp.setExtrainfo(ldList.get(i).getExtraInfo());
            temp.setUsername(ldList.get(i).getUsername());
            temp.setPlatform(ldList.get(i).getPlatform());
            temp.setSeq(ldList.get(i).getSeq());
            temp.setEncryptOnDevice(ldList.get(i).getEncryptOnDevice());
            if (Boolean.TRUE.equals(ldList.get(i).getEncryptOnDevice())) {
                byte[] pw = ldList.get(i).getPassword();
                temp.setPassword(pw == null ? "" : new String(pw, StandardCharsets.UTF_8));
            } else {
                temp.setPassword("*************");
            }
            list.add(temp);
        }

        log.info("Returned {} login data entries", list.size());
        return list;
    }

    @PostMapping("/Data/GetClearPassword")
    public TempLoginData GetClearPassword(@RequestHeader("Authorization") String bearerToken,
                                          @Valid @RequestBody NewData data) {
        log.info("Fetching clear password for seq: {}", data.getTmpLoginData().getSeq());
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);

        TempLoginData temp = data.getTmpLoginData();
        Logindata ldData = logDataRepository.findBySeq(data.getTmpLoginData().getSeq());

        if (ldData == null || !ldData.getUserSeq().equals(activeuser.getUserseq())) {
            log.warn("Ownership check failed for seq: {} and user: {}", data.getTmpLoginData().getSeq(), activeuser.getUsername());
            throw new UnauthorizedException("Not authorized to access this login data");
        }

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());
        if (Boolean.TRUE.equals(ldData.getEncryptOnDevice())) {
            byte[] pw = ldData.getPassword();
            temp.setPassword(pw == null ? "" : new String(pw, StandardCharsets.UTF_8));
            return temp;
        }
        try {
            byte[] key = Crypt.deriveKeyFromPassphrase(data.getPassphrase(), dbUser.getVaultSalt());
            temp.setPassword(Crypt.decrypt(ldData.getPassword(), key, ldData.getNonce()));
        } catch (Exception e) {
            log.error("Failed to decrypt password for seq: {}", data.getTmpLoginData().getSeq(), e);
            throw new BadRequestException("Could not decrypt login data. Check your passphrase.");
        }

        return temp;
    }
}