package com.av.agents.approval.infra;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingConductorEventPublisher implements ConductorEventPublisher {

  @Override
  public void publish(String eventName, Map<String, Object> payload) {
    log.info("Publishing Conductor event={} payload={}", eventName, payload);
  }
}
