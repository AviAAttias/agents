package com.av.agents.notificationworker.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "emailFrom", "from@example.com");
    }

    @Test
    void send_rendersTemplateIntoEmailMessage() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String providerMessageId = notificationService.send("recipient@example.com", "Financial Report Approved - Job 42", "Summary Totals: 99");

        assertThat(providerMessageId).startsWith("smtp-");
        assertThat(mimeMessage.getSubject()).isEqualTo("Financial Report Approved - Job 42");
        assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo("from@example.com");
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("recipient@example.com");
        assertThat(mimeMessage.getContent().toString()).contains("Summary Totals: 99");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void send_retriesOnceOnTransientSmtpFailure() {
        MimeMessage first = new MimeMessage((jakarta.mail.Session) null);
        MimeMessage second = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(first, second);
        doThrow(new MailSendException("421 temporary failure"))
                .doNothing()
                .when(mailSender)
                .send(any(MimeMessage.class));

        String providerMessageId = notificationService.send("recipient@example.com", "subject", "body");

        assertThat(providerMessageId).startsWith("smtp-");
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }
}
