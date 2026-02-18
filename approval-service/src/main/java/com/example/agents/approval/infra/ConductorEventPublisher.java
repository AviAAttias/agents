package com.example.agents.approval.infra;

import java.util.Map;

public interface ConductorEventPublisher {

  void publish(String eventName, Map<String, Object> payload);
}
