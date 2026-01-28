package edu.gju.chatbot.gju_chatbot.etl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

@SpringBootTest
public class OcrScannerIT {

    private Resource resource = new DefaultResourceLoader().getResource(
        "classpath:file_source.pdf"
    );

    @Autowired
    OcrScanner ocrScanner;

    @Test
    void convertFileToMarkdown() {
        Document markdown = ocrScanner.scan(resource);

        assertTrue(markdown.getText().length() > 0);
        assertTrue(markdown.getMetadata().get("file_id") != null);
        assertEquals(
            (String) markdown.getMetadata().get("file_name"),
            resource.getFilename()
        );
    }
}
