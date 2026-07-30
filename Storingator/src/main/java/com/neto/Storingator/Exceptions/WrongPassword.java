package com.neto.Storingator.Exceptions;

public class WrongPassword extends RuntimeException {

  public WrongPassword(String message) {
    super(message);
  }
}