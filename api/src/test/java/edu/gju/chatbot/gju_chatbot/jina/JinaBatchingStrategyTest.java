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
    assertTrue(batches.isEmpty());
  }

  @Test
  void testSingleBatchWithoutSummary() {
    List<Document> documents = List.of(
        new Document("Small content 1"),
        new Document("Small content 2"),
        new Document("Small content 3"));

    List<List<Document>> batches = strategy.batch(documents);

    assertEquals(1, batches.size());
    assertEquals(3, batches.get(0).size());
    batches.get(0).forEach(doc -> assertEquals("TARGET", doc.getMetadata().get(CHUNK_TYPE_KEY)));
  }

  @Test
  void testSingleBatchWithSummary() {
    Document doc = new Document("Content", Map.of(FILE_SUMMARY_KEY, "Summary"));
    List<List<Document>> batches = strategy.batch(List.of(doc));

    assertEquals(1, batches.size());
    assertEquals(2, batches.get(0).size());
    assertEquals("FILE_SUMMARY", batches.get(0).get(0).getMetadata().get(CHUNK_TYPE_KEY));
    assertEquals("TARGET", batches.get(0).get(1).getMetadata().get(CHUNK_TYPE_KEY));
  }

  @Test
  void testMultipleBatchesWithOverlap() {
    StringBuilder largeContent = new StringBuilder();
    for (int i = 0; i < 500; i++) {
      largeContent.append("Document content here. ");
    }
    String summary = "Summary";

    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      documents.add(new Document(largeContent.toString() + " " + i,
          Map.of(FILE_SUMMARY_KEY, summary)));
    }

    List<List<Document>> batches = strategy.batch(documents);

    assertTrue(batches.size() > 1, "Should create multiple batches");

    assertEquals("FILE_SUMMARY", batches.get(0).get(0).getMetadata().get(CHUNK_TYPE_KEY));

    for (int i = 1; i < batches.size(); i++) {
      List<Document> batch = batches.get(i);
      assertEquals("FILE_SUMMARY", batch.get(0).getMetadata().get(CHUNK_TYPE_KEY));
      assertEquals("OVERLAP", batch.get(1).getMetadata().get(CHUNK_TYPE_KEY));
    }
  }

  @Test
  void testConstraint1_FileSummaryExceedsTokenLimit() {
    StringBuilder largeSummary = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      largeSummary.append("This is a very long summary with many words. ");
    }

    Document doc = new Document("Content", Map.of(FILE_SUMMARY_KEY, largeSummary.toString()));

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> strategy.batch(List.of(doc)));
    assertTrue(exception.getMessage().contains("File summary exceeds"));
  }

  @Test
  void testConstraint2_DocumentTooLarge() {
    StringBuilder veryLargeContent = new StringBuilder();
    for (int i = 0; i < 5000; i++) {
      veryLargeContent.append("Very large content block with many words. ");
    }

    List<Document> documents = List.of(new Document(veryLargeContent.toString()));

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> strategy.batch(documents));
    assertTrue(exception.getMessage().contains("too big"));
  }

  @Test
  void testConstraint3_ContextSqueeze() {
    StringBuilder mediumText = new StringBuilder();
    for (int i = 0; i < 750; i++) {
      mediumText.append("Some content here. ");
    }

    String summary = mediumText.toString();
    String content = mediumText.toString();

    List<Document> documents = List.of(
        new Document(content, Map.of(FILE_SUMMARY_KEY, summary)),
        new Document(content, Map.of(FILE_SUMMARY_KEY, summary)));

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> strategy.batch(documents));
    assertTrue(exception.getMessage().contains("Context Squeeze"));
  }

  @Test
  void testCustomTokenLimit() {
    JinaBatchingStrategy customStrategy = new JinaBatchingStrategy(2000);

    StringBuilder content = new StringBuilder();
    for (int i = 0; i < 233; i++) {
      content.append("Document content. ");
    }

    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      documents.add(new Document(content.toString()));
    }

    List<List<Document>> batches = customStrategy.batch(documents);
    assertTrue(batches.size() >= 2, "Lower limit should force more batches");
  }

  @Test
  void testMetadataPreservation() {
    Map<String, Object> metadata = Map.of("custom_key", "value", "number", 123);
    Document doc = new Document("Content", metadata);

    List<List<Document>> batches = strategy.batch(List.of(doc));
    Document processed = batches.get(0).get(0);

    assertEquals("value", processed.getMetadata().get("custom_key"));
    assertEquals(123, processed.getMetadata().get("number"));
    assertNotNull(processed.getMetadata().get(CHUNK_TYPE_KEY));
  }
}
