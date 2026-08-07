package com.neto.Storingator.Controller;

import com.neto.Storingator.Entities.Appuser;
import com.neto.Storingator.Service.SessionService;
import com.neto.Storingator.Service.UserService;
import com.neto.Storingator.Utilities.SessionResponse;
import com.neto.Storingator.Utilities.TempUser;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private SessionService sessionService;

    @GetMapping("/User/")
    public String initUserGet() {
        return "Initiate Session by giving User and Password.";
    }

    @PostMapping("/User/New")
    public ResponseEntity<String> createNewUser(@Valid @RequestBody TempUser tmpuser) {
        log.info("Received new user creation request for: {}", tmpuser.getUsername());
        Appuser user = new Appuser();
        userService.createNewUser(user, tmpuser);
        log.info("New user creation request processed for: {}", tmpuser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body("User created");
    }

    @PostMapping("/User/Signin")
    @Transactional
    public SessionResponse SignIn(@Valid @RequestBody TempUser tmpuser) {
        log.info("Sign in request for user: {}", tmpuser.getUsername());
        return userService.signIn(tmpuser);
    }

    @PostMapping("/User/Logout")
    public ResponseEntity<String> Logout(@RequestHeader("Authorization") String bearerToken) {
        sessionService.logout(bearerToken);
        return ResponseEntity.ok("Logged out");
    }
}