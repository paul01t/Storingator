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
        if (newLoginData.getPassword() == null || newLoginData.getPassword().isEmpty()) {
            throw new BadRequestException("Password is required");
        }

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());

        Logindata loginData = new Logindata();
        loginData.setUsername(newLoginData.getUsername());
        loginData.setEmail(newLoginData.getEmail());
        loginData.setExtraInfo(newLoginData.getExtrainfo());
        loginData.setPlatform(newLoginData.getPlatform());
        loginData.setUserSeq(activeuser.getUserseq());

        try {
            byte[] nonceBT = Crypt.getRandomNonce();
            byte[] key = Crypt.deriveKeyFromPassphrase(data.getPassphrase(), dbUser.getVaultSalt());
            byte[] passwordByte = newLoginData.getPassword().getBytes(StandardCharsets.UTF_8);
            loginData.setPassword(Crypt.encrypt(passwordByte, key, nonceBT));
            loginData.setNonce(nonceBT);
        } catch (Exception e) {
            log.error("Failed to encrypt login data for user: {}", dbUser.getUsername(), e);
            throw new VaultOperationFailed("Failed to encrypt login data");
        }

        logDataRepository.save(loginData);
        log.info("Login data saved successfully");
        return "Login data saved";
    }

    @PostMapping("/Data/GetLoginData")
    public List<TempLoginData> GetLoginData(@RequestHeader("Authorization") String bearerToken) {
        Activeusers activeuser = sessionService.authenticateFromBearer(bearerToken);
        log.info("Fetching login data for user: {}", activeuser.getUsername());
        List<TempLoginData> list = new ArrayList<>();
        List<Logindata> ldList = logDataRepository.findByUserseq(activeuser.getUserseq());

        for (int i = 0; i < ldList.size(); i++) {
            TempLoginData temp = new TempLoginData();
            temp.setEmail(ldList.get(i).getEmail());
            temp.setExtrainfo(ldList.get(i).getExtraInfo());
            temp.setUsername(ldList.get(i).getUsername());
            temp.setPlatform(ldList.get(i).getPlatform());
            temp.setPassword("*************");
            temp.setSeq(ldList.get(i).getSeq());
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