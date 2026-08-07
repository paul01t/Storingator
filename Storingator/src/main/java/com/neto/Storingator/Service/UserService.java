package com.neto.Storingator.Service;

import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Entities.Appuser;
import com.neto.Storingator.Exceptions.BadRequestException;
import com.neto.Storingator.Exceptions.NewUserCreationFailed;
import com.neto.Storingator.Exceptions.UnauthorizedException;
import com.neto.Storingator.Exceptions.UserAlreadyExistsException;
import com.neto.Storingator.Utilities.Crypt;
import com.neto.Storingator.Utilities.SessionResponse;
import com.neto.Storingator.Entities.TempUser;
import com.neto.Storingator.Repository.ActiveUsersRepository;
import com.neto.Storingator.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
@Slf4j
public class UserService {
    private static final Duration SIGNIN_WINDOW = Duration.ofMinutes(15);
    private static final int SIGNIN_MAX_ATTEMPTS = 5;
    private static final Duration SIGNUP_WINDOW = Duration.ofHours(1);
    private static final int SIGNUP_MAX_ATTEMPTS = 5;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActiveUsersRepository acUsersRepository;
    @Autowired
    private AuthRateLimitService rateLimitService;

    public void createNewUser(Appuser user, TempUser tmpuser) {
        String username = tmpuser.getUsername().trim();
        String email = tmpuser.getEmail() == null ? null : tmpuser.getEmail().trim();
        log.info("Creating new user: {}", username);

        if (email == null || email.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        rateLimitService.check("signup:" + username.toLowerCase(Locale.ROOT) + ":" + email.toLowerCase(Locale.ROOT),
                SIGNUP_MAX_ATTEMPTS, SIGNUP_WINDOW);

        if (userRepository.findByUsername(username) != null) {
            throw new UserAlreadyExistsException("Username already taken");
        }
        if (userRepository.findByEmail(email) != null) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        String password = tmpuser.getClearPassword();
        if (password == null || password.isEmpty()) {
            throw new BadRequestException("Password cannot be empty");
        }

        try {
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to create user: {}", username, e);
            throw new NewUserCreationFailed("New user creation failed!");
        }
        log.info("User created successfully: {}", username);
    }

    public SessionResponse signIn(TempUser tmpuser) {
        String username = tmpuser.getUsername().trim();
        log.info("Sign in attempt for user: {}", username);
        rateLimitService.check("signin:global", 100, Duration.ofMinutes(15));

        Appuser dbUser = userRepository.findByUsername(username);
        if (dbUser == null) {
            throw new UnauthorizedException("Invalid username or password");
        }

        rateLimitService.check("signin:" + username.toLowerCase(Locale.ROOT), SIGNIN_MAX_ATTEMPTS, SIGNIN_WINDOW);

        if (dbUser.getPassword() == null || !passwordEncoder.matches(tmpuser.getClearPassword(), dbUser.getPassword())) {
            rateLimitService.recordFailure("signin:" + username.toLowerCase(Locale.ROOT), SIGNIN_WINDOW);
            log.warn("Wrong password for user: {}", username);
            throw new UnauthorizedException("Invalid username or password");
        }

        acUsersRepository.deleteByUserseq(dbUser.getId());

        Activeusers newAU = new Activeusers();
        newAU.setLoginkey(Crypt.randomPassphrase(50, Crypt.characters));
        newAU.setUserseq(dbUser.getId());
        newAU.setUsername(dbUser.getUsername());
        acUsersRepository.save(newAU);
        log.info("Sign in successful for user: {}", username);
        return new SessionResponse(dbUser.getUsername(), newAU.getLoginkey());
    }
}