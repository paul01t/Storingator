package com.neto.Storingator.Exceptions;

public class VaultOperationFailed extends RuntimeException {

  public VaultOperationFailed(String message) {
    super(message);
  }

  public VaultOperationFailed(String message, Throwable cause) {
    super(message, cause);
  }
}