package com.neto.Storingator.Controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Entities.Appuser;
import com.neto.Storingator.Entities.Logindata;
import com.neto.Storingator.Utilities.Crypt;
import com.neto.Storingator.Utilities.NewData;
import com.neto.Storingator.Utilities.TempLoginData;
import com.neto.Storingator.Utilities.TempUser;
import com.neto.Storingator.Repository.ActiveUsersRepository;
import com.neto.Storingator.Repository.LoginDataRepository;
import com.neto.Storingator.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginDataController {

    private static final Logger log = LoggerFactory.getLogger(LoginDataController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoginDataRepository logDataRepository;
    @Autowired
    private ActiveUsersRepository acUsersRepository;

    @PostMapping("/Data/New")
    public String createLoginData(@RequestBody NewData data) {
        log.info("Received new login data request");
        Activeusers acuser = data.getAc();
        TempLoginData newLoginData = data.getTmpLoginData();
        String passphrase = data.getPassphrase();

        Activeusers activeuser = acUsersRepository.getActiveUsersByUsername(acuser.getUsername());

        if (Objects.isNull(activeuser)){
            log.warn("User did not log in: {}", acuser.getUsername());
            return "User is not logged in";
        }

        if (!acuser.getLoginkey().equals(activeuser.getLoginkey())) {
            log.warn("Validation failed for user: {}", acuser.getUsername());
            return "Validation Failed" + acuser.getSeq() + " ," + activeuser.getSeq();
        }

        Appuser dbUser = userRepository.findByUsername(activeuser.getUsername());
        TempUser tmpuser = new TempUser();
        tmpuser.setClearPassword(passphrase);

        Logindata loginData = new Logindata();
        loginData.setUsername(newLoginData.getUsername());
        loginData.setEmail(newLoginData.getEmail());
        loginData.setExtraInfo(newLoginData.getExtrainfo());
        loginData.setPlatform(newLoginData.getPlatform());
        loginData.setUserSeq(dbUser.getId());

        try {
            byte[] nonceBT = Crypt.getRandomNonce();
            byte[] key = Crypt.keyGenerateEncodedFromUser(dbUser, tmpuser);
            byte[] passwordByte = newLoginData.getPassword().getBytes(StandardCharsets.UTF_8);
            loginData.setPassword(Crypt.encrypt(passwordByte, key, nonceBT));
            loginData.setNonce(nonceBT);
        } catch (Exception e) {
            log.error("Failed to encrypt and save login data for user: {}", dbUser.getUsername(), e);
        }

        logDataRepository.save(loginData);
        log.info("Login data saved successfully");
        return "Post Request Received";
    }

    @GetMapping("/Data/GetLoginData")
    public List<TempLoginData> GetLoginData(@RequestBody Activeusers user) {
        log.info("Fetching login data for user: {}", user.getUsername());
        List<TempLoginData> list = new ArrayList<>();
        List<Logindata> ldList = logDataRepository.findByUserseq(user.getUserseq());

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

    @GetMapping("/Data/GetClearPassword")
    public TempLoginData GetClearPassword(@RequestBody NewData data) {
        log.info("Fetching clear password for seq: {}", data.getTmpLoginData().getSeq());
        Activeusers acuser = data.getAc();
        Activeusers activeuser = acUsersRepository.getActiveUsersByUsername(acuser.getUsername());

        if (!acuser.getLoginkey().equals(activeuser.getLoginkey())) {
            log.warn("Validation failed for user: {}", acuser.getUsername());
            TempLoginData fail = new TempLoginData();
            fail.setUsername("Failed!");
            return fail;
        }

        TempLoginData temp = data.getTmpLoginData();
        Logindata ldData = logDataRepository.findBySeq(data.getTmpLoginData().getSeq());
        Appuser dbUser = userRepository.findByUsername(data.getAc().getUsername());
        try {
            TempUser tmpuser = new TempUser();
            tmpuser.setClearPassword(data.getPassphrase());
            byte[] key = Crypt.keyGenerateEncodedFromUser(dbUser, tmpuser);
            temp.setPassword(Crypt.decrypt(ldData.getPassword(), key, ldData.getNonce()));
        } catch (Exception e) {
            log.error("Failed to decrypt password for seq: {}", data.getTmpLoginData().getSeq(), e);
        }

        return temp;
    }
}