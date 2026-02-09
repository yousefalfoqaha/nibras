package edu.gju.chatbot.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DocumentSearchService {

  private static final Logger log = LoggerFactory.getLogger(
      DocumentSearchService.class);

  private final VectorStoreRetriever retriever;

  private final DocumentTransformer documentTransformer;

  public List<Document> search(UserQuery query) {
    String filter = buildFilterExpression(query);
    log.info("Applied Filter: [{}]", filter != null ? filter : "NONE");

    List<Document> similarChunks = retriever.similaritySearch(
        SearchRequest.builder()
            .query(query.getQuery())
            .similarityThreshold(0.4)
            .filterExpression(filter)
            .topK(5)
            .build());

    for (Document c : similarChunks) {
      System.out.println(c.getFormattedContent());
    }

    log.info("Vector Store returned {} raw chunks.", similarChunks.size());

    if (similarChunks.isEmpty()) {
      return List.of();
    }

    return documentTransformer.transform(similarChunks);
  }

  private String buildFilterExpression(UserQuery query) {
    List<String> filterParts = new ArrayList<>();

    if (query.getConfirmedAttributes() != null) {
      for (Map.Entry<String, Object> entry : query
          .getConfirmedAttributes()
          .entrySet()) {
        String key = entry.getKey();
        Object val = entry.getValue();
        String condition = (val instanceof String)
            ? String.format("%s == '%s'", key, val)
            : String.format("%s == %s", key, val);

        filterParts.add(condition);
      }
    }

    if (query.getDocumentType() != null &&
        !query.getDocumentType().isBlank()) {
      filterParts.add(
          "document_type == '" + query.getDocumentType() + "'");
    }

    if (query.getTargetYear() != null) {
      filterParts.add("year == " + query.getTargetYear());
    }

    return filterParts.isEmpty() ? null : String.join(" && ", filterParts);
  }

}
