package com.av.agents.notificationworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.notificationworker.dto.PipelineStepRequestDto;
import com.av.agents.notificationworker.worker.NotificationWorker;
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
