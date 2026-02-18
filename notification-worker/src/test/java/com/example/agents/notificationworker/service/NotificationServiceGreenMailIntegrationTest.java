package com.example.agents.notificationworker.service;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationServiceGreenMailIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
        registry.add("EMAIL_FROM", () -> "noreply@example.com");
    }

    @Autowired
    private NotificationService notificationService;

    @Test
    void send_deliversMessageViaSmtp() throws Exception {
        notificationService.send("recipient@example.com", "Financial Report Approved - Job 99", "Summary Totals: 100");

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("Financial Report Approved - Job 99");
        assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("recipient@example.com");
        assertThat(messages[0].getContent().toString()).contains("Summary Totals: 100");
    }
}
