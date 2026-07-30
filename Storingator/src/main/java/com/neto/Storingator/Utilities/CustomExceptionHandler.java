package com.neto.Storingator.Utilities;

import com.neto.Storingator.Exceptions.NewUserCreationFailed;
import com.neto.Storingator.Exceptions.WrongPassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomExceptionHandler.class);

    @ExceptionHandler(NewUserCreationFailed.class)
    public ResponseEntity<String> handleBadRequest(NewUserCreationFailed ex) {
        log.error("New user creation failed", ex);
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(WrongPassword.class)
    public ResponseEntity<String> handleBadRequest(WrongPassword ex) {
        log.error("Wrong password", ex);
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}