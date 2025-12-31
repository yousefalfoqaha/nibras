package edu.gju.chatbot.gju_chatbot.reader;

import java.util.UUID;
import java.util.function.Function;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class MarkdownConverter implements Function<Resource, Document> {

  private final RestClient restClient;

  public MarkdownConverter(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public Document apply(Resource file) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", file);

    ConverterApiResponse apiResponse = restClient.post()
        .body(parts)
        .retrieve()
        .body(ConverterApiResponse.class);

    Document document = new Document(apiResponse.content);

    document.getMetadata().put(DocumentMetadataKeys.FILE_ID, UUID.randomUUID());
    document.getMetadata().put(DocumentMetadataKeys.FILE_NAME, file.getFilename());
    document.getMetadata().put(DocumentMetadataKeys.FILE_SIZE, apiResponse.fileSize());

    return document;
  }

  public record ConverterApiResponse(
      @JsonProperty("file_name") String fileName,
      @JsonProperty("file_size") int fileSize,
      @JsonProperty("content") String content) {
  }
}
