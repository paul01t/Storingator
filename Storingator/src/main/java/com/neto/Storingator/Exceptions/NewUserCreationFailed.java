package com.neto.Storingator.Exceptions;

public class NewUserCreationFailed extends RuntimeException {

  public NewUserCreationFailed(String message) {
    super(message);
  }
}