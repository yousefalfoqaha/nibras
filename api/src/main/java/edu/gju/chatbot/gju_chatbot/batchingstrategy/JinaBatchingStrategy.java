package edu.gju.chatbot.gju_chatbot.batchingstrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class JinaBatchingStrategy implements BatchingStrategy {

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
    this.safeMaxInputTokenCount = (int) Math
        .floor(maxInputTokenCount * (1 - TOKEN_COUNT_RESERVE_PERCENTAGE));
  }

  @Override
  public List<List<Document>> batch(List<Document> documents) {
    if (documents.isEmpty())
      return List.of();

    String fileSummary = (String) documents.get(0).getMetadata().get(DocumentMetadataKeys.FILE_SUMMARY);

    List<List<Document>> batches = new ArrayList<>();
    List<Document> batch = new ArrayList<>();
    String batchBreadcrumbs = (String) documents.get(0).getMetadata().get(DocumentMetadataKeys.BREADCRUMBS);

    int summaryTokens = (fileSummary != null) ? tokenCountEstimator.estimate(fileSummary) : 0;
    int batchTokens = 0;
    int batchOverlapTokens = 0;
    int batchBreadcrumbsTokens = tokenCountEstimator.estimate(batchBreadcrumbs);

    if (summaryTokens + batchBreadcrumbsTokens > safeMaxInputTokenCount) {
      throw new IllegalArgumentException("File summary and batch breadcrumbs exceeds total allowed token limit.");
    }

    if (fileSummary != null) {
      batch.add(copyAndLabel(new Document(fileSummary), ChunkType.FILE_SUMMARY));
      batchTokens += summaryTokens;
    }

    batch.add(copyAndLabel(new Document(batchBreadcrumbs), ChunkType.BREADCRUMBS));
    batchTokens += batchBreadcrumbsTokens;

    for (int i = 0; i < documents.size(); i++) {
      Document document = documents.get(i);
      String documentBreadcrumbs = (String) document.getMetadata().get(DocumentMetadataKeys.BREADCRUMBS);
      int documentTokens = tokenCountEstimator.estimate(document.getText());

      boolean addingDocumentExceedsBatchLimit = batchTokens + documentTokens > safeMaxInputTokenCount;
      boolean isNewSection = !documentBreadcrumbs.equals(batchBreadcrumbs);

      if (addingDocumentExceedsBatchLimit || isNewSection) {
        batches.add(new ArrayList<>(batch));
        batch.clear();
        batchTokens = 0;
        batchBreadcrumbsTokens = 0;
        batchOverlapTokens = 0;

        if (fileSummary != null) {
          batch.add(copyAndLabel(new Document(fileSummary), ChunkType.FILE_SUMMARY));
          batchTokens += summaryTokens;
        }

        if (isNewSection) {
          int documentBreadcrumbsTokens = tokenCountEstimator.estimate(documentBreadcrumbs);

          batchBreadcrumbs = documentBreadcrumbs;
          batchBreadcrumbsTokens = documentBreadcrumbsTokens;
        }

        batch.add(copyAndLabel(new Document(batchBreadcrumbs), ChunkType.BREADCRUMBS));
        batchTokens += batchBreadcrumbsTokens;

        if (summaryTokens + batchBreadcrumbsTokens + batchOverlapTokens + documentTokens > safeMaxInputTokenCount) {
          throw new IllegalArgumentException("Document is too big for any batch.");
        }

        if (i > 0 && !isNewSection) {
          Document overlap = documents.get(i - 1);
          batchOverlapTokens = tokenCountEstimator.estimate(overlap.getText());

          batch.add(copyAndLabel(overlap, ChunkType.OVERLAP));
          batchTokens += batchOverlapTokens;
        }
      }

      if (summaryTokens + batchBreadcrumbsTokens + batchOverlapTokens + documentTokens > safeMaxInputTokenCount) {
        throw new IllegalArgumentException(String.format(
            "Context Squeeze at index %d: Summary (%d) + Overlap (%d) + Doc (%d) = %d tokens, which exceeds the limit of %d",
            i, summaryTokens, batchOverlapTokens, documentTokens, (summaryTokens + batchOverlapTokens + documentTokens),
            safeMaxInputTokenCount));
      }

      batch.add(copyAndLabel(document, ChunkType.TARGET));
      batchTokens += documentTokens;
    }

    if (!batch.isEmpty())
      batches.add(batch);

    return batches;
  }

  private Document copyAndLabel(Document doc, ChunkType type) {
    Map<String, Object> metadata = new LinkedHashMap<>(doc.getMetadata());
    metadata.put(CHUNK_TYPE_KEY, type.name());

    return new Document(doc.getId(), doc.getText(), metadata);
  }
}
