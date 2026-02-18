package com.example.agents.approval.service;

import java.time.Instant;

public record DecisionResult(String decision, String reviewer, String comment, Instant decidedAt) {}
