package edu.gju.chatbot.gju_chatbot.jina;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JinaBatchingStrategyTest {

  private JinaBatchingStrategy strategy;
  private static final String FILE_SUMMARY_KEY = "file_summary";
  private static final String CHUNK_TYPE_KEY = "chunk_type";

  @BeforeEach
  void setUp() {
    strategy = new JinaBatchingStrategy();
  }

  @Test
  void testEmptyDocumentList() {
    List<Document> documents = List.of();
    List<List<Document>> batches = strategy.batch(documents);

    assertTrue(batches.isEmpty(), "Empty document list should return empty batches");
  }

  @Test
  void testSingleDocumentWithoutSummary() {
    Document doc = new Document("Small document content");
    List<List<Document>> batches = strategy.batch(List.of(doc));

    assertEquals(1, batches.size(), "Should create one batch");
    assertEquals(1, batches.get(0).size(), "Batch should contain one document");
    assertEquals("TARGET", batches.get(0).get(0).getMetadata().get(CHUNK_TYPE_KEY));
  }

  @Test
  void testSingleDocumentWithSummary() {
    Document doc = new Document("Document content", Map.of(FILE_SUMMARY_KEY, "Summary text"));
    List<List<Document>> batches = strategy.batch(List.of(doc));

    assertEquals(1, batches.size(), "Should create one batch");
    assertEquals(2, batches.get(0).size(), "Batch should contain summary and document");
    assertEquals("FILE_SUMMARY", batches.get(0).get(0).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("TARGET", batches.get(0).get(1).getMetadata().get(CHUNK_TYPE_KEY));
  }

  @Test
  void testFileSummaryExceedsTokenLimit() {
    String largeSummary = "x".repeat(30000);
    Document doc = new Document("Content", Map.of(FILE_SUMMARY_KEY, largeSummary));

    assertThrows(IllegalArgumentException.class, () -> strategy.batch(List.of(doc)),
        "Should throw exception when file summary exceeds token limit");
  }

  @Test
  void testContextSqueeze() {
    String mediumSummary = "s".repeat(10000); // ~2500 tokens
    String largeContent = "x".repeat(20000); // ~5000 tokens each

    List<Document> documents = new ArrayList<>();
    documents.add(new Document(largeContent, Map.of(FILE_SUMMARY_KEY, mediumSummary)));
    documents.add(new Document(largeContent, Map.of(FILE_SUMMARY_KEY, mediumSummary)));

    assertThrows(IllegalArgumentException.class, () -> strategy.batch(documents),
        "Should throw exception when summary + overlap + document exceeds token limit");
  }

  @Test
  void testMultipleDocumentsInOneBatch() {
    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      documents.add(new Document("Small content " + i));
    }

    List<List<Document>> batches = strategy.batch(documents);

    assertEquals(1, batches.size(), "Should create one batch for small documents");
    assertEquals(5, batches.get(0).size(), "Batch should contain all 5 documents");
  }

  @Test
  void testMultipleBatches() {
    // Create documents that will require splitting into multiple batches
    String mediumContent = "x".repeat(5000); // ~1250 tokens each
    List<Document> documents = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      documents.add(new Document(mediumContent + " " + i));
    }

    List<List<Document>> batches = strategy.batch(documents);

    assertTrue(batches.size() > 1, "Should create multiple batches");

    for (List<Document> batch : batches) {
      assertFalse(batch.isEmpty(), "Each batch should contain at least one document");
    }
  }

  @Test
  void testOverlapDocumentsInSubsequentBatches() {
    String mediumContent = "x".repeat(6000); // ~1500 tokens each
    String summary = "Summary";

    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      documents.add(new Document(mediumContent + " " + i, Map.of(FILE_SUMMARY_KEY, summary)));
    }

    List<List<Document>> batches = strategy.batch(documents);

    assertTrue(batches.size() > 1, "Should create multiple batches");

    for (int i = 1; i < batches.size(); i++) {
      List<Document> batch = batches.get(i);
      long overlapCount = batch.stream()
          .filter(doc -> "OVERLAP".equals(doc.getMetadata().get(CHUNK_TYPE_KEY)))
          .count();

      assertEquals(1, overlapCount, "Each batch after the first should have exactly one overlap document");
    }
  }

  @Test
  void testSummaryInAllBatches() {
    String mediumContent = "x".repeat(6000); // ~1500 tokens each
    String summary = "File summary text";

    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      documents.add(new Document(mediumContent + " " + i, Map.of(FILE_SUMMARY_KEY, summary)));
    }

    List<List<Document>> batches = strategy.batch(documents);

    for (List<Document> batch : batches) {
      assertEquals("FILE_SUMMARY", batch.get(0).getMetadata().get(CHUNK_TYPE_KEY),
          "Each batch should start with file summary");
      assertEquals(summary, batch.get(0).getText(),
          "Summary text should match original");
    }
  }

  @Test
  void testCustomTokenLimit() {
    JinaBatchingStrategy customStrategy = new JinaBatchingStrategy(4000);

    String content = "x".repeat(12000); // ~3000 tokens
    List<Document> documents = List.of(
        new Document(content),
        new Document(content));

    List<List<Document>> batches = customStrategy.batch(documents);

    assertTrue(batches.size() >= 2, "Custom lower limit should create more batches");
  }

  @Test
  void testMetadataPreservation() {
    Map<String, Object> originalMetadata = Map.of(
        "custom_key", "custom_value",
        "another_key", 123);

    Document doc = new Document("Content", originalMetadata);
    List<List<Document>> batches = strategy.batch(List.of(doc));

    Document processedDoc = batches.get(0).get(0);
    assertEquals("custom_value", processedDoc.getMetadata().get("custom_key"),
        "Original metadata should be preserved");
    assertEquals(123, processedDoc.getMetadata().get("another_key"),
        "Original metadata should be preserved");
    assertNotNull(processedDoc.getMetadata().get(CHUNK_TYPE_KEY),
        "Chunk type should be added");
  }

  @Test
  void testDocumentAtTokenLimit() {
    // 7372 tokens * 4 chars/token ≈ 29,488 characters
    String largeButSafeContent = "x".repeat(29000);

    Document doc = new Document(largeButSafeContent);

    assertDoesNotThrow(() -> {
      List<List<Document>> batches = strategy.batch(List.of(doc));
      assertFalse(batches.isEmpty(), "Should successfully create batches");
    });
  }
}
