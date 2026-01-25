package edu.gju.chatbot.gju_chatbot.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import edu.gju.chatbot.gju_chatbot.metadata.MetadataKeys;

class OverlapBatchingStrategyTest {

  private static final Logger logger = LoggerFactory.getLogger(OverlapBatchingStrategyTest.class);

  private OverlapBatchingStrategy strategy;
  private TokenCountEstimator tokenizer;

  private static final String CHUNK_TYPE_KEY = "chunk_type";

  private static final String SUMMARY_TEXT = "Summary of the file content here.";

  private static final String BREADCRUMBS = "Course > Section A";

  private int summaryCost;
  private int breadcrumbCost;
  private int docCost;

  @BeforeEach
  void setUp() {
    tokenizer = new JTokkitTokenCountEstimator();

    summaryCost = tokenizer.estimate(SUMMARY_TEXT);
    breadcrumbCost = tokenizer.estimate(BREADCRUMBS);

    String baseText = "test ".repeat(40);
    docCost = tokenizer.estimate(baseText);

    logger.info("--- Test Setup Metrics ---");
    logger.info("Summary Cost: {}", summaryCost);
    logger.info("Breadcrumb Cost: {}", breadcrumbCost);
    logger.info("Standard Doc Cost: {}", docCost);

    int targetCapacity = summaryCost + breadcrumbCost + (docCost * 2);

    int maxInputTokenCount = (int) Math.ceil(targetCapacity / 0.9);

    strategy = new OverlapBatchingStrategy(maxInputTokenCount);

    logger.info("Configured Strategy Max Tokens: {}", maxInputTokenCount);
  }

  @Test
  @DisplayName("Scenario: 5 Docs of same section. Should overlap cleanly.")
  void testStandardOverlap() {
    List<Document> docs = generateDocs(5, BREADCRUMBS);

    List<List<Document>> batches = strategy.batch(docs);

    logger.info("Generated {} batches", batches.size());

    assertTrue(batches.size() >= 2, "Should create multiple batches");

    List<Document> batch1 = batches.get(0);
    assertChunkType(batch1.get(0), "FILE_SUMMARY");
    assertChunkType(batch1.get(1), "BREADCRUMBS");
    assertChunkType(batch1.get(2), "TARGET");
    assertChunkType(batch1.get(3), "TARGET");

    List<Document> batch2 = batches.get(1);
    assertChunkType(batch2.get(0), "FILE_SUMMARY");
    assertChunkType(batch2.get(1), "BREADCRUMBS");

    Document overlapDoc = batch2.get(2);
    assertChunkType(overlapDoc, "OVERLAP");
    assertEquals(docs.get(1).getText(), overlapDoc.getText(), "Overlap doc should match Doc 1 from previous batch");
  }

  @Test
  @DisplayName("Scenario: Giant Document Protection")
  void testGiantDocumentThrowsException() {
    String giantText = "huge ".repeat(1000);
    Document giantDoc = new Document(giantText, Map.of(
        MetadataKeys.FILE_SUMMARY, SUMMARY_TEXT,
        MetadataKeys.BREADCRUMBS, BREADCRUMBS));

    try {
      strategy.batch(List.of(giantDoc));
    } catch (IllegalArgumentException e) {
      logger.info("Caught expected exception: {}", e.getMessage());
      assertTrue(e.getMessage().contains("exceeds limit"));
    }
  }

  private List<Document> generateDocs(int count, String breadcrumbs) {
    String docText = "test ".repeat(40);
    return IntStream.range(0, count)
        .mapToObj(i -> new Document(docText, Map.of(
            MetadataKeys.FILE_SUMMARY, SUMMARY_TEXT,
            MetadataKeys.BREADCRUMBS, breadcrumbs)))
        .collect(Collectors.toList());
  }

  private void assertChunkType(Document doc, String expectedType) {
    String actual = (String) doc.getMetadata().get(CHUNK_TYPE_KEY);
    assertNotNull(actual, "Chunk type should not be null");
    assertEquals(expectedType, actual, "Chunk type mismatch for doc: " + doc.getText().substring(0, 10) + "...");
  }
}
