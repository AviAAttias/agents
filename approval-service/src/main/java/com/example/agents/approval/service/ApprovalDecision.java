package com.example.agents.approval.service;

import com.example.agents.approval.domain.ApprovalStatus;

public enum ApprovalDecision {
  APPROVED,
  REJECTED;

  public ApprovalStatus toStatus() {
    return ApprovalStatus.valueOf(name());
  }
}
