package com.av.agents.approval.infra;

import java.util.Map;

public interface IConductorEventPublisher {

  void publish(String eventName, Map<String, Object> payload);
}
