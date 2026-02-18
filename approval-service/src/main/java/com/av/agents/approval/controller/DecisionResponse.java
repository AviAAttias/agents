package com.av.agents.approval.controller;

import java.time.Instant;

public record DecisionResponse(String decision, String reviewer, String comment, Instant decidedAt) {}
