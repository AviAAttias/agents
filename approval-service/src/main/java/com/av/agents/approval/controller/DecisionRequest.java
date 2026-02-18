package com.av.agents.approval.controller;

import com.av.agents.approval.service.ApprovalDecision;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecisionRequest(
    @NotNull ApprovalDecision decision,
    @NotBlank @Email String reviewer,
    String comment) {}
