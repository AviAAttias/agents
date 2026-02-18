package com.av.agents.notificationworker.service;

public interface INotificationService {
    String send(String recipient, String subject, String body);
}
