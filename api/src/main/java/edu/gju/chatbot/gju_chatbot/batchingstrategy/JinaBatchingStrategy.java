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

  private static final Logger logger = LoggerFactory.getLogger(JinaBatchingStrategy.class);

  protected enum ChunkType {
    FILE_SUMMARY,
    TARGET,
    OVERLAP,
    BREADCRUMBS
  }

  private static final int DEFAULT_MAX_INPUT_TOKENS = 8191;
  private static final double TOKEN_COUNT_RESERVE_PERCENTAGE = 0.10;
  private static final String CHUNK_TYPE_KEY = "chunk_type";

  private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();
  private int safeMaxInputTokenCount;

  public JinaBatchingStrategy() {
    this(DEFAULT_MAX_INPUT_TOKENS);
  }

  public JinaBatchingStrategy(int maxInputTokenCount) {
    this.safeMaxInputTokenCount = (int) Math.floor(maxInputTokenCount * (1 - TOKEN_COUNT_RESERVE_PERCENTAGE));
    logger.info("Initialized JinaBatchingStrategy. Max Input: {}, Safe Limit ({}% reserve): {}",
        maxInputTokenCount, (TOKEN_COUNT_RESERVE_PERCENTAGE * 100), safeMaxInputTokenCount);
  }

  @Override
  public List<List<Document>> batch(List<Document> documents) {
    if (documents.isEmpty()) {
      logger.warn("Batching called with empty document list.");
      return List.of();
    }

    logger.info("=== Starting Batching Process for {} Documents ===", documents.size());

    String fileSummary = (String) documents.get(0).getMetadata().get(DocumentMetadataKeys.FILE_SUMMARY);
    String batchBreadcrumbs = (String) documents.get(0).getMetadata().get(DocumentMetadataKeys.BREADCRUMBS);

    int summaryTokens = (fileSummary != null) ? tokenCountEstimator.estimate(fileSummary) : 0;
    int batchBreadcrumbsTokens = tokenCountEstimator.estimate(batchBreadcrumbs);

    logger.debug("Global File Summary Tokens: {}", summaryTokens);
    logger.debug("Initial Breadcrumbs Tokens: {}", batchBreadcrumbsTokens);

    if (summaryTokens + batchBreadcrumbsTokens > safeMaxInputTokenCount) {
      logger.error("FATAL: Summary ({}) + Breadcrumbs ({}) > Safe Limit ({})",
          summaryTokens, batchBreadcrumbsTokens, safeMaxInputTokenCount);
      throw new IllegalArgumentException("File summary and batch breadcrumbs exceeds total allowed token limit.");
    }

    List<List<Document>> batches = new ArrayList<>();
    List<Document> batch = new ArrayList<>();

    int batchTokens = 0;
    int batchOverlapTokens = 0;
    int currentBatchIndex = 1;

    logger.info("--- [BATCH #1 STARTED] ---");

    if (fileSummary != null) {
      batch.add(copyAndLabel(new Document(fileSummary), ChunkType.FILE_SUMMARY));
      batchTokens += summaryTokens;
      logger.debug("   + Added FILE_SUMMARY ({} tokens) | Total: {}", summaryTokens, batchTokens);
    }

    batch.add(copyAndLabel(new Document(batchBreadcrumbs), ChunkType.BREADCRUMBS));
    batchTokens += batchBreadcrumbsTokens;
    logger.debug("   + Added BREADCRUMBS ({} tokens) | Total: {}", batchBreadcrumbsTokens, batchTokens);

    for (int i = 0; i < documents.size(); i++) {
      Document document = documents.get(i);
      String documentBreadcrumbs = (String) document.getMetadata().get(DocumentMetadataKeys.BREADCRUMBS);
      int documentTokens = tokenCountEstimator.estimate(document.getText());

      boolean addingDocumentExceedsBatchLimit = batchTokens + documentTokens > safeMaxInputTokenCount;
      boolean isNewSection = !documentBreadcrumbs.equals(batchBreadcrumbs);

      if (addingDocumentExceedsBatchLimit || isNewSection) {
        String reason = isNewSection ? "NEW SECTION DETECTED" : "TOKEN LIMIT EXCEEDED";
        logger.info("--- [BATCH #{}] FINALIZED. Reason: {} (Size: {}/{}) ---",
            currentBatchIndex, reason, batchTokens, safeMaxInputTokenCount);

        batches.add(new ArrayList<>(batch));
        currentBatchIndex++;

        batch.clear();
        batchTokens = 0;
        batchBreadcrumbsTokens = 0;
        batchOverlapTokens = 0;

        logger.info("--- [BATCH #{} STARTED] ---", currentBatchIndex);

        if (fileSummary != null) {
          batch.add(copyAndLabel(new Document(fileSummary), ChunkType.FILE_SUMMARY));
          batchTokens += summaryTokens;
          logger.debug("   + Added FILE_SUMMARY ({} tokens)", summaryTokens);
        }

        if (isNewSection) {
          logger.debug("   ! Updating Breadcrumbs for new section.");
          int documentBreadcrumbsTokens = tokenCountEstimator.estimate(documentBreadcrumbs);
          batchBreadcrumbs = documentBreadcrumbs;
          batchBreadcrumbsTokens = documentBreadcrumbsTokens;
        }

        batch.add(copyAndLabel(new Document(batchBreadcrumbs), ChunkType.BREADCRUMBS));
        batchTokens += batchBreadcrumbsTokens;
        logger.debug("   + Added BREADCRUMBS ({} tokens) | Total: {}", batchBreadcrumbsTokens, batchTokens);

        if (summaryTokens + batchBreadcrumbsTokens + batchOverlapTokens + documentTokens > safeMaxInputTokenCount) {
          logger.error("FATAL: Document too big for new batch logic.");
          throw new IllegalArgumentException("Document is too big for any batch.");
        }

        if (i > 0 && !isNewSection) {
          Document overlap = documents.get(i - 1);
          batchOverlapTokens = tokenCountEstimator.estimate(overlap.getText());

          batch.add(copyAndLabel(overlap, ChunkType.OVERLAP));
          batchTokens += batchOverlapTokens;
          logger.debug("   + Added OVERLAP ({} tokens) | Total: {}", batchOverlapTokens, batchTokens);
        }
      }

      if (summaryTokens + batchBreadcrumbsTokens + batchOverlapTokens + documentTokens > safeMaxInputTokenCount) {
        int totalReq = summaryTokens + batchOverlapTokens + documentTokens;
        logger.error("FATAL: Context Squeeze at index {}. Req: {}, Limit: {}", i, totalReq, safeMaxInputTokenCount);
        throw new IllegalArgumentException(String.format(
            "Context Squeeze at index %d: Summary (%d) + Overlap (%d) + Doc (%d) = %d tokens, which exceeds the limit of %d",
            i, summaryTokens, batchOverlapTokens, documentTokens, totalReq, safeMaxInputTokenCount));
      }

      batch.add(copyAndLabel(document, ChunkType.TARGET));
      batchTokens += documentTokens;
      logger.debug("   > Added TARGET Doc [{}] ({} tokens) | Batch Total: {}/{}",
          i, documentTokens, batchTokens, safeMaxInputTokenCount);
    }

    if (!batch.isEmpty()) {
      logger.info("--- [BATCH #{}] FINALIZED (Final Batch) (Size: {}/{}) ---",
          currentBatchIndex, batchTokens, safeMaxInputTokenCount);
      batches.add(batch);
    }

    logger.info("=== Batching Complete. Total Batches: {} ===", batches.size());
    return batches;
  }

  private Document copyAndLabel(Document document, ChunkType type) {
    Map<String, Object> metadata = new LinkedHashMap<>(document.getMetadata());
    metadata.put(CHUNK_TYPE_KEY, type.name());
    return new Document(document.getId(), document.getText(), metadata);
  }
}
