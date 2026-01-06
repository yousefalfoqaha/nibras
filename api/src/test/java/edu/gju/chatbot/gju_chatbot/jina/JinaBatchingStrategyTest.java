package edu.gju.chatbot.gju_chatbot.jina;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import edu.gju.chatbot.gju_chatbot.batchingstrategy.JinaBatchingStrategy;
import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JinaBatchingStrategyTest {

  private List<Document> testDocuments;

  private int caseOneBatchTokenLimit;

  private int caseTwoBatchTokenLimit;

  private JinaBatchingStrategy strategyCaseOne;

  private JinaBatchingStrategy strategyCaseTwo;

  private static final String FILE_SUMMARY_KEY = DocumentMetadataKeys.FILE_SUMMARY;
  private static final String BREADCRUMBS_KEY = DocumentMetadataKeys.BREADCRUMBS;
  private static final String CHUNK_TYPE_KEY = "chunk_type";

  @BeforeEach
  void setUp() {
    String summaryText = "This is just a summary.";
    String documentText = "This is a sample text created for testing purposes.";
    String breadcrumbsText = "These are sample breadcrumbs.";
    String differentBreadcrumbsText = "These are totally different breadcrumbs";

    Map<String, Object> metadata1 = Map.of(FILE_SUMMARY_KEY, summaryText, BREADCRUMBS_KEY, breadcrumbsText);
    Map<String, Object> metadata2 = Map.of(FILE_SUMMARY_KEY, summaryText, BREADCRUMBS_KEY, differentBreadcrumbsText);

    List<Document> documents = List.of(
        new Document(documentText, metadata1),
        new Document(documentText, metadata2));

    TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

    int summaryTokens = tokenCountEstimator.estimate(summaryText);
    int documentTokens = tokenCountEstimator.estimate(documentText);
    int breadcrumbsTokens = tokenCountEstimator.estimate(breadcrumbsText);
    int differentBreadcrumbsTokens = tokenCountEstimator.estimate(differentBreadcrumbsText);

    this.caseOneBatchTokenLimit = (int) Math.floor((summaryTokens + breadcrumbsTokens + (documentTokens * 3)) * 1.15);
    this.caseTwoBatchTokenLimit = (int) Math
        .floor((summaryTokens + differentBreadcrumbsTokens + (documentTokens * 3)) * 1.15);

    this.testDocuments = documents;

    int maxInputForCaseOne = (int) Math.ceil(this.caseOneBatchTokenLimit / 0.9);
    int maxInputForCaseTwo = (int) Math.ceil(this.caseTwoBatchTokenLimit / 0.9);

    this.strategyCaseOne = new JinaBatchingStrategy(maxInputForCaseOne);
    this.strategyCaseTwo = new JinaBatchingStrategy(maxInputForCaseTwo);
  }

  @Test
  void testTwoDocumentsWithDifferentBreadcrumbsProduceTwoBatches_caseOneStrategy() {
    List<List<Document>> batches = strategyCaseOne.batch(testDocuments);

    assertEquals(2, batches.size(), "Different breadcrumbs should start a new batch");

    List<Document> firstBatch = batches.get(0);
    assertTrue(firstBatch.size() >= 3, "first batch should contain at least FILE_SUMMARY, BREADCRUMBS, TARGET");
    assertEquals("FILE_SUMMARY", firstBatch.get(0).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("BREADCRUMBS", firstBatch.get(1).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("TARGET", firstBatch.get(2).getMetadata().get(CHUNK_TYPE_KEY));

    List<Document> secondBatch = batches.get(1);
    assertTrue(secondBatch.size() >= 3, "second batch should contain at least FILE_SUMMARY, BREADCRUMBS, TARGET");
    assertEquals("FILE_SUMMARY", secondBatch.get(0).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("BREADCRUMBS", secondBatch.get(1).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("TARGET", secondBatch.get(2).getMetadata().get(CHUNK_TYPE_KEY));
  }

  @Test
  void testOverlapWithExistingStrategy() {
    List<Document> documents = List.of(
        testDocuments.get(0),
        testDocuments.get(0),
        testDocuments.get(0),
        testDocuments.get(0));

    List<List<Document>> batches = strategyCaseOne.batch(documents);

    assertTrue(batches.size() > 1, "Second document should create a new batch and trigger overlap");

    List<Document> secondBatch = batches.get(1);

    assertEquals("FILE_SUMMARY", secondBatch.get(0).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("BREADCRUMBS", secondBatch.get(1).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("OVERLAP", secondBatch.get(2).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("TARGET", secondBatch.get(3).getMetadata().get(CHUNK_TYPE_KEY));
  }
}
