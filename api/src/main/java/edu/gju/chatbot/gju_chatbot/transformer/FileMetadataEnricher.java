package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;

import edu.gju.chatbot.gju_chatbot.metadata.MetadataFilter;
import edu.gju.chatbot.gju_chatbot.metadata.MetadataFilterRepository;
import edu.gju.chatbot.gju_chatbot.metadata.MetadataKeys;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileMetadataEnricher implements Function<Document, Document> {

  private static Logger log = LoggerFactory.getLogger(FileMetadataEnricher.class);

  private static final PromptTemplate SYSTEM_PROMPT_TEMPLATE = new PromptTemplate(
      """
                  You are extracting useful metadata from a document for retrieval in a knowledge base.

                  Create a clear, descriptive title for the provided text. It should serve as a label for the file, this title is the value for the "title" key in the structured output.

                  Extract metadata filters ONLY from the list below, and place them as key (filter name) and value pairs:

                  METADATA_FILTERS:
                  <<<
                  {metadata_filters}
                  >>>
          """);

  private final ChatClient chatClient;

  private final MetadataFilterRepository metadataFilterRepository;

  public Document enrich(Document document) {
    return apply(document);
  }

  @Override
  public Document apply(Document document) {
    List<MetadataFilter> metadataFilters = metadataFilterRepository.fetchMetadataFilters();
    String formattedMetadataFilters = metadataFilters
        .stream()
        .map(MetadataFilter::getFormattedFilter)
        .collect(Collectors.joining("\n\n"));

    EnrichedMetadata enrichedMetadata = this.chatClient.prompt()
        .user(u -> u.text(document.getText()))
        .system(s -> s.text(SYSTEM_PROMPT_TEMPLATE.render(Map.of("metadata_filters", formattedMetadataFilters))))
        .call()
        .entity(EnrichedMetadata.class);

    log.debug("Summary generated: {}", enrichedMetadata);

    document.getMetadata().put(MetadataKeys.TITLE, enrichedMetadata.title());

    Map<String, MetadataFilter> allowedFilters = metadataFilters.stream()
        .collect(Collectors.toMap(MetadataFilter::getName, f -> f));

    for (Map.Entry<String, String> entry : enrichedMetadata.filters().entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (allowedFilters.containsKey(key)) {
        document.getMetadata().put(key, value);
      } else {
        log.warn("AI returned unknown metadata key: {}", key);
      }
    }

    return document;
  }

  private record EnrichedMetadata(
      String title,
      Map<String, String> filters) {
  };
}
