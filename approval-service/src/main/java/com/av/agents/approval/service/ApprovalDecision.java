package com.av.agents.approval.service;

import com.av.agents.sharedpersistence.entity.ApprovalStatus;

public enum ApprovalDecision {
  APPROVED,
  REJECTED;

  public ApprovalStatus toStatus() {
    return ApprovalStatus.valueOf(name());
  }
}
