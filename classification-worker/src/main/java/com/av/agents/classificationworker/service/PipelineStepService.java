package com.av.agents.classificationworker.service;

import com.av.agents.classificationworker.dto.PipelineStepRequestDto;
import com.av.agents.classificationworker.entity.ClassificationArtifactEntity;
import com.av.agents.classificationworker.entity.PipelineStepEntity;
import com.av.agents.sharedpersistence.entity.TextArtifactEntity;
import com.av.agents.classificationworker.mapper.IPipelineStepMapper;
import com.av.agents.classificationworker.repository.IClassificationArtifactRepository;
import com.av.agents.classificationworker.repository.IPipelineStepRepository;
import com.av.agents.sharedpersistence.repository.ITextArtifactRepository;
import com.av.agents.common.ai.OpenAiJsonClient;
import com.av.agents.common.ai.OpenAiJsonClientException;
import com.av.agents.common.ai.OpenAiJsonRequest;
import com.av.agents.common.ai.OpenAiJsonResponse;
import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.common.enums.PipelineStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineStepService implements IPipelineStepService {
    private static final String SCHEMA_NAME = "classification_document_type_v1";

    private final IPipelineStepRepository pipelineStepRepository;
    private final IPipelineStepMapper pipelineStepMapper;
    private final ITextArtifactRepository textArtifactRepository;
    private final IClassificationArtifactRepository classificationArtifactRepository;
    private final OpenAiJsonClient openAiJsonClient;
    private final ObjectMapper objectMapper;

    @Value("${classification.worker.max-text-chars:${ai.operations.classification.max-text-chars:12000}}")
    private int maxTextChars;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Override
    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        String idempotencyKey = requestDto.getJobId() + ":" + requestDto.getTaskType();
        Optional<PipelineStepEntity> existing = pipelineStepRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent() && existing.get().getPayloadJson() != null && !existing.get().getPayloadJson().isBlank()) {
            PipelineStepEntity cached = existing.get();
            cached.setUpdatedAt(OffsetDateTime.now());
            pipelineStepRepository.save(cached);
            return toMessage(cached);
        }

        PipelineStepEntity entity = existing.orElseGet(() -> pipelineStepMapper.toEntity(requestDto));
        ClassificationExecutionResult execution = classify(requestDto);

        entity.setPayloadJson(execution.outputJson());
        entity.setStatus(PipelineStatus.PROCESSED.name());
        entity.setUpdatedAt(OffsetDateTime.now());
        pipelineStepRepository.save(entity);

        persistArtifact(entity.getJobId(), entity.getTaskType(), execution);
        return toMessage(entity);
    }

    private ClassificationExecutionResult classify(PipelineStepRequestDto requestDto) {
        long startMs = System.currentTimeMillis();
        try {
            JsonNode input = objectMapper.readTree(requestDto.getPayloadJson());
            String artifactRef = input.path("textArtifact").asText("").trim();
            if (artifactRef.isBlank()) {
                throw new PipelineTaskException("INVALID_INPUT", "textArtifact is required");
            }

            String sourceText = resolveTextArtifact(artifactRef);
            boolean inputTruncated = sourceText.length() > maxTextChars;
            String boundedText = inputTruncated ? sourceText.substring(0, maxTextChars) : sourceText;

            String requestId = UUID.randomUUID().toString();
            OpenAiJsonResponse response = openAiJsonClient.completeJson(OpenAiJsonRequest.builder()
                    .operation("classification")
                    .schemaName(SCHEMA_NAME)
                    .systemPrompt("Classify financial documents. Return only JSON with documentType.")
                    .userPrompt("Classify this financial document text.\n\n" + boundedText)
                    .jsonSchema(classificationSchema())
                    .requestId(requestId)
                    .jobId(requestDto.getJobId())
                    .taskId(requestDto.getTaskType())
                    .maxInputChars(maxTextChars)
                    .build());

            String documentType = mapDocumentType(response.getContent());
            long durationMs = System.currentTimeMillis() - startMs;
            ObjectNode output = objectMapper.createObjectNode();
            output.put("documentType", documentType);
            output.put("model", openAiModel);
            output.put("durationMs", durationMs);
            output.put("inputChars", boundedText.length());
            output.put("outputChars", response.getOutputChars());
            output.put("schemaName", SCHEMA_NAME);
            output.put("requestId", requestId);
            output.put("inputTruncated", inputTruncated || response.isInputTruncated());

            return new ClassificationExecutionResult(
                    objectMapper.writeValueAsString(output),
                    objectMapper.writeValueAsString(response.getContent()),
                    objectMapper.writeValueAsString(objectMapper.createObjectNode().put("documentType", documentType))
            );
        } catch (OpenAiJsonClientException ex) {
            throw new PipelineTaskException(ex.getErrorCode().name(), "classification LLM call failed", ex);
        } catch (PipelineTaskException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PipelineTaskException("INVALID_INPUT", "classification payload parse failed", ex);
        }
    }

    private String resolveTextArtifact(String artifactRef) {
        String prefix = "text-artifact://";
        if (!artifactRef.startsWith(prefix)) {
            throw new PipelineTaskException("INVALID_INPUT", "unsupported textArtifact ref: " + artifactRef);
        }
        String rawId = artifactRef.substring(prefix.length());
        long id;
        try {
            id = Long.parseLong(rawId);
        } catch (NumberFormatException ex) {
            throw new PipelineTaskException("INVALID_INPUT", "invalid textArtifact id: " + artifactRef, ex);
        }
        TextArtifactEntity artifact = textArtifactRepository.findById(id)
                .orElseThrow(() -> new PipelineTaskException("ARTIFACT_NOT_FOUND", "text artifact not found: " + artifactRef));
        if (artifact.getTextBody() == null || artifact.getTextBody().isBlank()) {
            throw new PipelineTaskException("INVALID_INPUT", "resolved text artifact is blank: " + artifactRef);
        }
        return artifact.getTextBody();
    }

    private String mapDocumentType(JsonNode content) {
        String documentType = content.path("documentType").asText("").trim();
        if (documentType.isBlank()) {
            throw new PipelineTaskException("INVALID_SCHEMA_OUTPUT", "documentType is missing from schema-valid output");
        }
        return documentType;
    }

    private JsonNode classificationSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("documentType", objectMapper.createObjectNode().put("type", "string").put("minLength", 1));

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(java.util.List.of("documentType")));
        schema.put("additionalProperties", false);
        return schema;
    }

    private void persistArtifact(String jobId, String taskType, ClassificationExecutionResult execution) {
        ClassificationArtifactEntity artifact = new ClassificationArtifactEntity();
        artifact.setJobId(jobId);
        artifact.setTaskType(taskType);
        artifact.setRawResponseJson(execution.rawResponseJson());
        artifact.setMappedResultJson(execution.mappedResultJson());
        artifact.setCreatedAt(OffsetDateTime.now());
        classificationArtifactRepository.save(artifact);
    }

    private PipelineMessageDto toMessage(PipelineStepEntity entity) {
        return PipelineMessageDto.builder()
                .jobId(entity.getJobId())
                .taskType(entity.getTaskType())
                .status(PipelineStatus.PROCESSED)
                .artifactRef(entity.getArtifactRef())
                .payloadJson(entity.getPayloadJson())
                .processedAt(entity.getUpdatedAt())
                .build();
    }

    private record ClassificationExecutionResult(String outputJson, String rawResponseJson, String mappedResultJson) {
    }
}
