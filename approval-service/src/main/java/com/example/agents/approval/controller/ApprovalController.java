package com.example.agents.approval.controller;

import com.example.agents.approval.facade.ApprovalFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalController {

  private final ApprovalFacade approvalFacade;

  @PostMapping("/{jobId}/decision")
  public DecisionResponse decide(@PathVariable String jobId, @Valid @RequestBody DecisionRequest request) {
    var result = approvalFacade.decide(jobId, request.decision(), request.reviewer(), request.comment());
    return new DecisionResponse(result.decision(), result.reviewer(), result.comment(), result.decidedAt());
  }
}
