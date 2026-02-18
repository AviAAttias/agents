package com.example.agents.notificationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.notificationworker.dto.PipelineStepRequestDto;
import com.example.agents.notificationworker.worker.NotificationWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFacade implements IPipelineFacade {

    private final NotificationWorker notificationWorker;

    @Override
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        return notificationWorker.process(requestDto);
    }
}
