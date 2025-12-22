package edu.gju.chatbot.gju_chatbot.jina;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

public class JinaBatchingStrategy implements BatchingStrategy {

  private static final int JINA_MAX_INPUT_TOKEN_COUNT = 8192;
  private static final double TOKEN_COUNT_RESERVE_PERCENTAGE = 0.10;
  private static final String FILE_SUMMARY_KEY = "file_summary";

  private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

  private final int safeMaxInputTokenCount = (int) Math
      .floor(JINA_MAX_INPUT_TOKEN_COUNT * (1 - TOKEN_COUNT_RESERVE_PERCENTAGE));

  @Override
  public List<List<Document>> batch(List<Document> documents) {

    if (documents == null || documents.isEmpty()) {
      return List.of();
    }

    int fileSummaryTokenCount = 0;
    Object summary = documents.get(0).getMetadata().get(FILE_SUMMARY_KEY);

    if (!(summary instanceof String s) || s.isBlank()) {
    } else {
      fileSummaryTokenCount = tokenCountEstimator.estimate(s);
    }

    if (fileSummaryTokenCount > safeMaxInputTokenCount) {
      throw new IllegalArgumentException(
          "file_summary exceeds maximum allowed token count");
    }

    Map<Document, Integer> documentTokens = new LinkedHashMap<>();

    for (Document document : documents) {
      int tokenCount = tokenCountEstimator
          .estimate(document.getFormattedContent());

      if (tokenCount > safeMaxInputTokenCount) {
        throw new IllegalArgumentException(
            "Single document exceeds maximum allowed token count");
      }

      documentTokens.put(document, tokenCount);
    }

    List<List<Document>> batches = new ArrayList<>();
    List<Document> currentBatch = new ArrayList<>();

    int currentSize = fileSummaryTokenCount;

    for (int i = 0; i < documents.size(); i++) {
      Document doc = documents.get(i);
      int docTokens = documentTokens.get(doc);

      if (currentSize + docTokens <= safeMaxInputTokenCount) {
        currentBatch.add(doc);
        currentSize += docTokens;
        continue;
      }

      if (!currentBatch.isEmpty()) {
        batches.add(currentBatch);
      }

      currentBatch = new ArrayList<>();
      currentSize = fileSummaryTokenCount;

      if (i <= 0) {
        currentBatch.add(doc);
        currentSize += docTokens;
        continue;
      }

      Document overlap = documents.get(i - 1);
      int overlapTokens = documentTokens.get(overlap);

      if (fileSummaryTokenCount + overlapTokens > safeMaxInputTokenCount) {
        currentBatch.add(doc);
        currentSize += docTokens;
        continue;
      }

      currentBatch.add(overlap);
      currentSize += overlapTokens;

      currentBatch.add(doc);
      currentSize += docTokens;
    }

    if (!currentBatch.isEmpty()) {
      batches.add(currentBatch);
    }

    return batches;
  }
}
