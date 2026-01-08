package edu.gju.chatbot.gju_chatbot.batchingstrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class JinaBatchingStrategy implements BatchingStrategy {

  private static final Logger log = LoggerFactory.getLogger(JinaBatchingStrategy.class);
  private static final int DEFAULT_MAX_INPUT_TOKENS = 8191;
  private static final double TOKEN_COUNT_RESERVE_PERCENTAGE = 0.10;
  private static final String CHUNK_TYPE_KEY = "chunk_type";

  protected enum ChunkType {
    FILE_SUMMARY, TARGET, OVERLAP, BREADCRUMBS
  }

  private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();
  private final int safeMaxInputTokenCount;

  public JinaBatchingStrategy() {
    this(DEFAULT_MAX_INPUT_TOKENS);
  }

  public JinaBatchingStrategy(int maxInputTokenCount) {
    this.safeMaxInputTokenCount = (int) Math.floor(maxInputTokenCount * (1 - TOKEN_COUNT_RESERVE_PERCENTAGE));
  }

  @Override
  public List<List<Document>> batch(List<Document> documents) {
    if (documents.isEmpty())
      return List.of();

    String summary = (String) documents.get(0).getMetadata().get(DocumentMetadataKeys.FILE_SUMMARY);
    int summaryTokens = (summary != null) ? tokenCountEstimator.estimate(summary) : 0;

    if (summaryTokens > safeMaxInputTokenCount) {
      throw new IllegalArgumentException("FATAL: File Summary alone exceeds token limit.");
    }

    log.info("Starting batching for {} documents. Limit: {}", documents.size(), safeMaxInputTokenCount);

    return recursiveBatch(documents, summary, summaryTokens, 0);
  }

  private List<List<Document>> recursiveBatch(List<Document> docs, String summary, int summaryTokens,
      int overlapCount) {
    if (docs.isEmpty())
      return new ArrayList<>();

    List<List<Document>> result = new ArrayList<>();
    List<Document> currentBatch = new ArrayList<>();
    int currentTokens = 0;

    if (summary != null) {
      currentBatch.add(copyAndLabel(new Document(summary), ChunkType.FILE_SUMMARY));
      currentTokens += summaryTokens;
    }

    Document headDoc = docs.get(0);
    String currentBreadcrumbs = (String) headDoc.getMetadata().get(DocumentMetadataKeys.BREADCRUMBS);
    int breadcrumbTokens = tokenCountEstimator.estimate(currentBreadcrumbs);
    int headDocTokens = tokenCountEstimator.estimate(headDoc.getText());

    if (currentTokens + breadcrumbTokens + headDocTokens > safeMaxInputTokenCount) {
      throw new IllegalArgumentException("Single document (Head) exceeds limit. Cannot proceed.");
    }

    currentBatch.add(copyAndLabel(new Document(currentBreadcrumbs), ChunkType.BREADCRUMBS));

    ChunkType headType = (overlapCount > 0) ? ChunkType.OVERLAP : ChunkType.TARGET;
    currentBatch.add(copyAndLabel(headDoc, headType));
    currentTokens += (breadcrumbTokens + headDocTokens);

    int splitIndex = -1;
    boolean splitDueToSectionChange = false;

    for (int i = 1; i < docs.size(); i++) {
      Document nextDoc = docs.get(i);
      String nextBreadcrumbs = (String) nextDoc.getMetadata().get(DocumentMetadataKeys.BREADCRUMBS);
      int nextTokens = tokenCountEstimator.estimate(nextDoc.getText());

      boolean isNewSection = !nextBreadcrumbs.equals(currentBreadcrumbs);
      boolean isFull = (currentTokens + nextTokens > safeMaxInputTokenCount);

      if (isNewSection || isFull) {
        splitIndex = i;
        splitDueToSectionChange = isNewSection;
        log.debug("Flushing batch at index {}. Reason: {}", i, isNewSection ? "Section Change" : "Full");
        break;
      }

      ChunkType docType = (i < overlapCount) ? ChunkType.OVERLAP : ChunkType.TARGET;
      currentBatch.add(copyAndLabel(nextDoc, docType));
      currentTokens += nextTokens;
    }

    result.add(currentBatch);

    if (splitIndex != -1) {
      int nextStartIndex;
      int newOverlapAmount;

      if (splitDueToSectionChange) {
        nextStartIndex = splitIndex;
        newOverlapAmount = 0;
      } else {
        nextStartIndex = Math.max(1, splitIndex - 1);
        newOverlapAmount = splitIndex - nextStartIndex;
      }

      result.addAll(recursiveBatch(
          docs.subList(nextStartIndex, docs.size()),
          summary,
          summaryTokens,
          newOverlapAmount));
    }

    return result;
  }

  private Document copyAndLabel(Document doc, ChunkType type) {
    Map<String, Object> metadata = new LinkedHashMap<>(doc.getMetadata());
    metadata.put(CHUNK_TYPE_KEY, type.name());
    return new Document(doc.getId(), doc.getText(), metadata);
  }
}
