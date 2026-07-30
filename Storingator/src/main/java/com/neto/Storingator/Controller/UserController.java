package com.neto.Storingator.Controller;

import com.neto.Storingator.Exceptions.NewUserCreationFailed;
import com.neto.Storingator.Service.UserService;
import com.neto.Storingator.Utilities.*;
import com.neto.Storingator.Repository.ActiveUsersRepository;
import com.neto.Storingator.Repository.UserRepository;
import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Entities.Appuser;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActiveUsersRepository acUsersRepository;
    @Autowired
    private UserService userService;

    @GetMapping("/User/")
    public String initUserGet() {
        return "Initiate Session by giving User and Password.";
    }

    @PostMapping("/User/New")
    public String createNewUser(@RequestBody TempUser tmpuser) {
        log.info("Received new user creation request for: {}", tmpuser.getUsername());
        Appuser user = new Appuser();
        user.setUsername(tmpuser.getUsername());
        user.setEmail(tmpuser.getEmail());
        userService.createNewUser(user, tmpuser);
        log.info("New user creation request processed for: {}", tmpuser.getUsername());
        return "Post Request Received";
    }

    @PostMapping("/User/Signin")
    @Transactional
    public @ResponseBody Activeusers SignIn(@RequestBody TempUser tmpuser) {
        log.info("Sign in request for user: {}", tmpuser.getUsername());
        return userService.signIn(tmpuser);
    }
}