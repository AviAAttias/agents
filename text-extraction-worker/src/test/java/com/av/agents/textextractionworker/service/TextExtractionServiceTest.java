package com.av.agents.textextractionworker.service;

import com.av.agents.common.ai.PipelineTaskException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextExtractionServiceTest {

    private PdfBoxTextExtractionService service;

    @BeforeEach
    void setUp() {
        service = new PdfBoxTextExtractionService(new SimpleMeterRegistry());
        ReflectionTestUtils.setField(service, "maxTextChars", 12000);
    }

    @Test
    void extract_extractsKnownTextFromPdf(@TempDir Path tempDir) throws Exception {
        Path pdf = tempDir.resolve("invoice.pdf");
        Files.copy(getClass().getResourceAsStream("/pdfs/invoice-sample.pdf"), pdf);

        PdfTextExtractionService.ExtractionResult result = service.extract(pdf.toString());

        assertThat(result.extractedText()).containsIgnoringCase("invoice");
        assertThat(result.pageCount()).isGreaterThan(0);
        assertThat(result.inputBytes()).isGreaterThan(0);
        assertThat(result.sha256()).hasSize(64);
        assertThat(result.wasTruncated()).isFalse();
    }

    @Test
    void extract_forImageOnlyPdf_returnsEmptyText(@TempDir Path tempDir) throws Exception {
        Path imageOnlyPdf = createImageOnlyPdf(tempDir.resolve("scan-only.pdf"));

        PdfTextExtractionService.ExtractionResult result = service.extract(imageOnlyPdf.toString());

        assertThat(result.pageCount()).isEqualTo(1);
        assertThat(result.extractedText().trim()).isEmpty();
        assertThat(result.wasTruncated()).isFalse();
    }

    @Test
    void extract_truncatesAtBoundary(@TempDir Path tempDir) throws Exception {
        ReflectionTestUtils.setField(service, "maxTextChars", 20);
        Path pdf = tempDir.resolve("invoice.pdf");
        Files.copy(getClass().getResourceAsStream("/pdfs/invoice-sample.pdf"), pdf);

        PdfTextExtractionService.ExtractionResult result = service.extract(pdf.toString());

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

    private Path createImageOnlyPdf(Path outputPath) throws Exception {
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.drawString("SCANNED INVOICE", 120, 100);
        graphics.dispose();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDImageXObject pdImage = JPEGFactory.createFromImage(document, image);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(pdImage, 72, 500, 300, 150);
            }
            document.save(outputPath.toFile());
        }
        return outputPath;
    }
}
