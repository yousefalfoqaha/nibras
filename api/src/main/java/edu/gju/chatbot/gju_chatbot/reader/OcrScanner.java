package edu.gju.chatbot.gju_chatbot.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import edu.gju.chatbot.gju_chatbot.exception.FileProcessingException;
import edu.gju.chatbot.gju_chatbot.utils.MetadataKeys;

public class OcrScanner implements Function<Resource, Document> {

  private static final Logger log = LoggerFactory.getLogger(OcrScanner.class);

  private final RestClient restClient;

  private final RetryTemplate retryTemplate;

  public OcrScanner(RestClient restClient, RetryTemplate retryTemplate) {
    this.restClient = restClient;
    this.retryTemplate = retryTemplate;
  }

  public Document scan(Resource file) {
    return this.apply(file);
  }

  @Override
  public Document apply(Resource file) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", file);

    String markdown = retryTemplate.execute(context -> restClient.post()
        .body(parts)
        .retrieve()
        .body(OcrScannerResponse.class)).content;

    UUID fileId = UUID.randomUUID();
    String fileName = file.getFilename();
    Map<String, Object> metadata = new HashMap<>();

    metadata.put(MetadataKeys.FILE_ID, fileId);
    metadata.put(MetadataKeys.FILE_NAME, fileName);
    try {
      metadata.put(MetadataKeys.FILE_SIZE, file.contentLength());
    } catch (IOException e) {
      throw new FileProcessingException("Failed to read file size.");
    }

    log.info("Scanned {} using OCR.", fileName);

    return new Document(markdown, metadata);
  }

  private record OcrScannerResponse(String content) {
  }
}
