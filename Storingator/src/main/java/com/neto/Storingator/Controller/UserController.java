package com.neto.Storingator.Controller;

import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Entities.Appuser;
import com.neto.Storingator.Entities.TempUser;
import com.neto.Storingator.Service.SessionService;
import com.neto.Storingator.Service.UserService;
import com.neto.Storingator.Utilities.ChangePasswordRequest;
import com.neto.Storingator.Utilities.DeleteUserRequest;
import com.neto.Storingator.Utilities.SessionResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/User")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private SessionService sessionService;

    @GetMapping("/")
    public String initUserGet() {
        return "Initiate Session by giving User and Password.";
    }

    @PostMapping("/New")
    public ResponseEntity<String> createNewUser(@Valid @RequestBody TempUser tmpuser) {
        log.info("Received new user creation request for: {}", tmpuser.getUsername());
        Appuser user = new Appuser();
        userService.createNewUser(user, tmpuser);
        log.info("New user creation request processed for: {}", tmpuser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body("User created");
    }

    @PostMapping("/Signin")
    public SessionResponse SignIn(@Valid @RequestBody TempUser tmpuser) {
        log.info("Sign in request for user: {}", tmpuser.getUsername());
        return userService.signIn(tmpuser);
    }

    @PostMapping("/Logout")
    public ResponseEntity<String> Logout(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        sessionService.logout(bearerToken);
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/Me")
    public ResponseEntity<SessionResponse> me(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        Activeusers active = sessionService.authenticateFromBearer(bearerToken);
        SessionResponse response = new SessionResponse();
        response.setUsername(active.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ChangePassword")
    public ResponseEntity<String> changePassword(@RequestHeader(value = "Authorization", required = false) String bearerToken,
                                                 @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(bearerToken, request);
        return ResponseEntity.ok("Password changed; please sign in again");
    }

    @PostMapping("/Delete")
    public ResponseEntity<String> deleteUser(@RequestHeader(value = "Authorization", required = false) String bearerToken,
                                             @Valid @RequestBody DeleteUserRequest request) {
        userService.deleteUser(bearerToken, request);
        return ResponseEntity.ok("User deleted");
    }
}