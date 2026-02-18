package com.av.agents.approvalservice.controller;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.approvalservice.dto.ApprovalPatchRequestDto;
import com.av.agents.approvalservice.dto.PipelineStepRequestDto;
import com.av.agents.approvalservice.facade.IPipelineFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final IPipelineFacade pipelineFacade;
    private final RestTemplate restTemplate;

    @Value("${conductor.event.endpoint:http://localhost:8080/api/events}")
    private String conductorEventEndpoint;

    @PostMapping("/request")
    public ResponseEntity<PipelineMessageDto> requestApproval(@Valid @RequestBody PipelineStepRequestDto requestDto) {
        return ResponseEntity.ok(pipelineFacade.process(requestDto));
    }

    @PatchMapping("/{jobId}")
    public ResponseEntity<Void> patchApproval(@PathVariable String jobId, @Valid @RequestBody ApprovalPatchRequestDto patchRequestDto) {
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("jobId", jobId);
        eventPayload.put("decision", patchRequestDto.getDecision());
        eventPayload.put("reviewer", patchRequestDto.getReviewer());
        eventPayload.put("patchedValues", patchRequestDto.getPatchedValues());
        restTemplate.postForEntity(conductorEventEndpoint + "/approval." + jobId, eventPayload, Void.class);
        return ResponseEntity.accepted().build();
    }
}
