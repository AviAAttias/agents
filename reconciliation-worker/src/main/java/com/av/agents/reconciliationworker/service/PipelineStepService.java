package com.av.agents.reconciliationworker.service;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.common.enums.PipelineStatus;
import com.av.agents.reconciliationworker.dto.PipelineStepRequestDto;
import com.av.agents.reconciliationworker.entity.PipelineStepEntity;
import com.av.agents.reconciliationworker.mapper.IPipelineStepMapper;
import com.av.agents.reconciliationworker.repository.IPipelineStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineStepService implements IPipelineStepService {
    private final IPipelineStepRepository pipelineStepRepository;
    private final IPipelineStepMapper pipelineStepMapper;
    private final OpenAiJsonClient openAiJsonClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        PipelineStepEntity entity = pipelineStepRepository.findByIdempotencyKey(requestDto.getJobId() + ":" + requestDto.getTaskType())
                .orElseGet(() -> pipelineStepMapper.toEntity(requestDto));
        entity.setPayloadJson(explainAnomalies(requestDto.getPayloadJson()));
        entity.setStatus(PipelineStatus.PROCESSED.name());
        entity.setUpdatedAt(OffsetDateTime.now());
        pipelineStepRepository.save(entity);
        return PipelineMessageDto.builder()
                .jobId(entity.getJobId())
                .taskType(entity.getTaskType())
                .status(PipelineStatus.PROCESSED)
                .artifactRef(entity.getArtifactRef())
                .payloadJson(entity.getPayloadJson())
                .processedAt(entity.getUpdatedAt())
                .build();
    }

    private String explainAnomalies(String payloadJson) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("isAnomaly", objectMapper.createObjectNode().put("type", "boolean"));
        properties.set("confidence", objectMapper.createObjectNode()
                .put("type", "number")
                .put("minimum", 0)
                .put("maximum", 1));
        properties.set("reasoning", objectMapper.createObjectNode().put("type", "string"));
        properties.set("recommendedAction", objectMapper.createObjectNode().put("type", "string"));

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(List.of(
                "isAnomaly", "confidence", "reasoning", "recommendedAction"
        )));
        schema.put("additionalProperties", false);

        return openAiJsonClient.completeJson(
                        "You are a reconciliation analyst expert. Explain mismatches and propose next action with concise reasoning.",
                        "Analyze reconciliation payload for anomalies and explain. Payload:\n" + payloadJson,
                        schema)
                .orElse("{\"isAnomaly\":false,\"confidence\":0.3,\"reasoning\":\"LLM unavailable; no explanation generated.\",\"recommendedAction\":\"run_rule_based_reconciliation\"}");
    }
}
