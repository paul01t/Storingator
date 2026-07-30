package com.neto.Storingator.Service;

import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Entities.Appuser;
import com.neto.Storingator.Exceptions.NewUserCreationFailed;
import com.neto.Storingator.Exceptions.WrongPassword;
import com.neto.Storingator.Utilities.Crypt;
import com.neto.Storingator.Utilities.TempUser;
import com.neto.Storingator.Repository.ActiveUsersRepository;
import com.neto.Storingator.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActiveUsersRepository acUsersRepository;

    public void createNewUser(Appuser user, TempUser tmpuser) {
        log.info("Creating new user: {}", user.getUsername());
        try {
            byte[] nonceBT = Crypt.getRandomNonce();
            byte[] key = Crypt.keyGenerateEncodedFromUser(user, tmpuser);
            byte[] passwordByte = tmpuser.getClearPassword().getBytes(StandardCharsets.UTF_8);
            user.setPassword(Crypt.encrypt(passwordByte, key, nonceBT));
            user.setNonce(nonceBT);
        } catch (Exception e) {
            log.error("Failed to create user: {}", user.getUsername(), e);
            throw new NewUserCreationFailed("New user creation failed!");
        }
        userRepository.save(user);
        log.info("User created successfully: {}", user.getUsername());
    }

    public Activeusers signIn(TempUser tmpuser) {
        log.info("Sign in attempt for user: {}", tmpuser.getUsername());
        Appuser dbUser = userRepository.findByUsername(tmpuser.getUsername());
        if (dbUser == null) {
            log.warn("User not found: {}", tmpuser.getUsername());
            Activeusers fail = new Activeusers();
            fail.setUsername("Failed");
            fail.setSeq(-1);
            return fail;
        }
        try {
            byte[] key = Crypt.keyGenerateEncodedFromUser(dbUser, tmpuser);
            byte[] passwordByte = tmpuser.getClearPassword().getBytes(StandardCharsets.UTF_8);
            byte[] encryptpsw = Crypt.encrypt(passwordByte, key, dbUser.getNonce());
            if (Arrays.equals(encryptpsw, dbUser.getPassword())) {
                Activeusers newAU = new Activeusers();
                newAU.setLoginkey(Crypt.randomPassphrase(50, Crypt.characters));
                newAU.setUserseq(dbUser.getId());
                newAU.setUsername(dbUser.getUsername());
                acUsersRepository.save(newAU);
                log.info("Sign in successful for user: {}", tmpuser.getUsername());
                return newAU;
            } else {
                log.warn("Wrong password for user: {}", tmpuser.getUsername());
                throw new WrongPassword("Wrong Password");
            }
        } catch (Exception e) {
            log.error("Sign in failed for user: {}", tmpuser.getUsername(), e);
            Activeusers fail = new Activeusers();
            fail.setUsername("Failed");
            fail.setSeq(-3);
            return fail;
        }
    }
}