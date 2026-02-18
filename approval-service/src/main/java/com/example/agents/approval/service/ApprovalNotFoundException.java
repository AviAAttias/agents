package com.example.agents.approval.service;

public class ApprovalNotFoundException extends RuntimeException {

  public ApprovalNotFoundException(String jobId) {
    super("Approval request not found for jobId=" + jobId);
  }
}
