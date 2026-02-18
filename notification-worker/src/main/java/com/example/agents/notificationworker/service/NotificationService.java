package com.example.agents.notificationworker.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private static final Pattern SMTP_4XX_PATTERN = Pattern.compile("\\b4\\d\\d\\b");

    private final JavaMailSender mailSender;

    @Value("${EMAIL_FROM:no-reply@example.com}")
    private String emailFrom;

    @Override
    public String send(String recipient, String subject, String body) {
        MailException firstFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                sendMime(recipient, subject, body);
                return "smtp-" + UUID.randomUUID();
            } catch (MailException ex) {
                if (attempt == 1 && isTransient(ex)) {
                    firstFailure = ex;
                    continue;
                }
                throw ex;
            }
        }
        throw firstFailure;
    }

    private void sendMime(String recipient, String subject, String body) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false);
            helper.setFrom(emailFrom);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to build SMTP message", e);
        }
    }

    private boolean isTransient(MailException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof MessagingException messagingException) {
                String detail = messagingException.getMessage();
                if (detail != null && SMTP_4XX_PATTERN.matcher(detail).find()) {
                    return true;
                }
            }
            current = current.getCause();
        }

        String message = ex.getMessage();
        return message != null && SMTP_4XX_PATTERN.matcher(message).find();
    }
}
