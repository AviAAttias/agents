package com.example.agents.approval.controller;

import com.example.agents.approval.service.ApprovalNotFoundException;
import com.example.agents.approval.service.InvalidApprovalStateException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(ApprovalNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleNotFound(ApprovalNotFoundException ex) {
    return Map.of("message", ex.getMessage());
  }

  @ExceptionHandler(InvalidApprovalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, String> handleState(InvalidApprovalStateException ex) {
    return Map.of("message", ex.getMessage());
  }
}
