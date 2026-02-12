package com.example.agents.textextractionworker.mapper;

import com.example.agents.textextractionworker.dto.PipelineStepRequestDto;
import com.example.agents.textextractionworker.entity.PipelineStepEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IPipelineStepMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PROCESSED")
    @Mapping(target = "artifactRef", expression = "java(\"artifact://\" + dto.getJobId() + \"/\" + dto.getTaskType())")
    @Mapping(target = "idempotencyKey", expression = "java(dto.getJobId() + \":\" + dto.getTaskType())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    PipelineStepEntity toEntity(PipelineStepRequestDto dto);
}
