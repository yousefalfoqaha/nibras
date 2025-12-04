package edu.gju.chatbot.gju_chatbot;

import java.util.List;
import java.util.function.Function;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class PdfDocumentReader implements Function<Resource, List<Document>> {

  @Override
  public List<Document> apply(Resource resource) {
    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
        PdfDocumentReaderConfig.builder()
            .withPageTopMargin(0)
            .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                .withNumberOfTopTextLinesToDelete(0)
                .build())
            .withPagesPerDocument(1)
            .build());

    return pdfReader.read();
  }
}
