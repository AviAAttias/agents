package com.example.agents.textextractionworker.service;

import com.example.agents.common.ai.PipelineTaskException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextExtractionServiceTest {

    private TextExtractionService service;

    @BeforeEach
    void setUp() {
        service = new TextExtractionService(new SimpleMeterRegistry());
        ReflectionTestUtils.setField(service, "maxTextChars", 12000);
    }

    @Test
    void extract_extractsKnownTextFromPdf(@TempDir Path tempDir) throws Exception {
        Path pdf = tempDir.resolve("invoice.pdf");
        Files.copy(getClass().getResourceAsStream("/pdfs/invoice-sample.pdf"), pdf);

        ITextExtractionService.ExtractionResult result = service.extract(pdf.toString());

        assertThat(result.extractedText()).containsIgnoringCase("invoice");
        assertThat(result.pageCount()).isGreaterThan(0);
        assertThat(result.inputBytes()).isGreaterThan(0);
        assertThat(result.sha256()).hasSize(64);
        assertThat(result.wasTruncated()).isFalse();
    }

    @Test
    void extract_truncatesAtBoundary(@TempDir Path tempDir) throws Exception {
        ReflectionTestUtils.setField(service, "maxTextChars", 20);
        Path pdf = tempDir.resolve("invoice.pdf");
        Files.copy(getClass().getResourceAsStream("/pdfs/invoice-sample.pdf"), pdf);

        ITextExtractionService.ExtractionResult result = service.extract(pdf.toString());

        assertThat(result.extractedText().length()).isEqualTo(20);
        assertThat(result.outputChars()).isEqualTo(20);
        assertThat(result.wasTruncated()).isTrue();
    }

    @Test
    void extract_throwsErrorForMalformedPdf(@TempDir Path tempDir) throws Exception {
        Path malformed = tempDir.resolve("malformed.pdf");
        Files.copy(getClass().getResourceAsStream("/pdfs/malformed.pdf"), malformed);

        assertThatThrownBy(() -> service.extract(malformed.toString()))
                .isInstanceOf(PipelineTaskException.class)
                .hasMessageContaining("cannot be parsed")
                .extracting("errorCode")
                .isEqualTo("PDF_MALFORMED");
    }
}
