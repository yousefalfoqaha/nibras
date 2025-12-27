package edu.gju.chatbot.gju_chatbot.batchingstrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

public class JinaBatchingStrategy implements BatchingStrategy {

  protected enum ChunkType {
    FILE_SUMMARY,
    TARGET,
    OVERLAP
  }

  private static final int DEFAULT_MAX_INPUT_TOKENS = 8191;

  private static final double TOKEN_COUNT_RESERVE_PERCENTAGE = 0.10;

  private static final String FILE_SUMMARY_KEY = "file_summary";

  private static final String CHUNK_TYPE_KEY = "chunk_type";

  private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

  private int safeMaxInputTokenCount;

  public JinaBatchingStrategy() {
    this(DEFAULT_MAX_INPUT_TOKENS);
  }

  public JinaBatchingStrategy(int maxInputTokenCount) {
    this.safeMaxInputTokenCount = (int) Math
        .floor(maxInputTokenCount * (1 - TOKEN_COUNT_RESERVE_PERCENTAGE));
  }

  @Override
  public List<List<Document>> batch(List<Document> documents) {
    if (documents.isEmpty())
      return List.of();

    String fileSummary = (String) documents.get(0).getMetadata().get(FILE_SUMMARY_KEY);
    int summaryTokens = (fileSummary != null) ? tokenCountEstimator.estimate(fileSummary) : 0;
    int overlapTokens = 0;

    if (summaryTokens > safeMaxInputTokenCount) {
      throw new IllegalArgumentException("File summary exceeds total allowed token limit.");
    }

    List<List<Document>> batches = new ArrayList<>();
    List<Document> currentBatch = new ArrayList<>();
    int currentBatchTokens = 0;

    if (fileSummary != null) {
      currentBatch.add(copyAndLabel(new Document(fileSummary), ChunkType.FILE_SUMMARY));
      currentBatchTokens += summaryTokens;
    }

    for (int i = 0; i < documents.size(); i++) {
      Document doc = documents.get(i);
      int docTokens = tokenCountEstimator.estimate(doc.getText());

      if (currentBatchTokens + docTokens > safeMaxInputTokenCount) {
        batches.add(new ArrayList<>(currentBatch));
        currentBatch.clear();
        currentBatchTokens = 0;

        if (fileSummary != null) {
          currentBatch.add(copyAndLabel(new Document(fileSummary), ChunkType.FILE_SUMMARY));
          currentBatchTokens += summaryTokens;
        }

        if (summaryTokens + overlapTokens + docTokens > safeMaxInputTokenCount) {
          throw new IllegalArgumentException("Document is too big for any batch.");
        }

        if (i > 0) {
          Document overlap = documents.get(i - 1);
          overlapTokens = tokenCountEstimator.estimate(overlap.getText());

          currentBatch.add(copyAndLabel(overlap, ChunkType.OVERLAP));
          currentBatchTokens += overlapTokens;
        }
      }

      if (summaryTokens + overlapTokens + docTokens > safeMaxInputTokenCount) {
        throw new IllegalArgumentException(String.format(
            "Context Squeeze at index %d: Summary (%d) + Overlap (%d) + Doc (%d) = %d tokens, which exceeds the limit of %d",
            i, summaryTokens, overlapTokens, docTokens, (summaryTokens + overlapTokens + docTokens),
            safeMaxInputTokenCount));
      }

      currentBatch.add(copyAndLabel(doc, ChunkType.TARGET));
      currentBatchTokens += docTokens;
    }

    if (!currentBatch.isEmpty())
      batches.add(currentBatch);

    return batches;
  }

  private Document copyAndLabel(Document doc, ChunkType type) {
    Map<String, Object> metadata = new LinkedHashMap<>(doc.getMetadata());
    metadata.put(CHUNK_TYPE_KEY, type.name());

    return new Document(doc.getId(), doc.getText(), metadata);
  }
}
