package com.av.agents.approval.service;

public class InvalidApprovalStateException extends RuntimeException {

  public InvalidApprovalStateException(String message) {
    super(message);
  }
}
