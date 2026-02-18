package com.example.agents.approval.service;

import java.time.Instant;

public record CreateApprovalRequest(String jobId, String reportArtifactRef, String reviewerEmail, Instant expiresAt) {}
