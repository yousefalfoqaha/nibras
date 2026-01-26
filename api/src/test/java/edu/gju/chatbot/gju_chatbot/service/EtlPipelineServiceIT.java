package edu.gju.chatbot.gju_chatbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.gju.chatbot.gju_chatbot.embedding.OverlapBatchingStrategy;
import edu.gju.chatbot.gju_chatbot.config.embedding.JinaEmbeddingProperties;
import edu.gju.chatbot.gju_chatbot.embedding.JinaEmbeddingModel;
import edu.gju.chatbot.gju_chatbot.embedding.JinaEmbeddingOptions;
import edu.gju.chatbot.gju_chatbot.transformer.FileMetadataEnricher;

@SpringBootTest(properties = {
    "spring.ai.jina.embedding.options.late-chunking=false",
    "spring.ai.vectorstore.pgvector.remove-existing-vector-store-table=true",
    "spring.datasource.url=jdbc:postgresql://db:5432/gju",
})
@EnabledIfEnvironmentVariable(named = "JINA_API_KEY", matches = ".+")
class EtlPipelineServiceIT {

  @Autowired
  JinaEmbeddingModel embeddingModel;

  @Autowired
  JinaEmbeddingProperties properties;

  @Autowired
  FileMetadataEnricher fileSummaryEnricher;

  @Autowired
  VectorStore vectorStore;

  @Test
  void testRequestToFileSummaryEnricher() {
    Document document = new Document("Some text that needs summarizing or whatever.");

    Document enrichedDocument = this.fileSummaryEnricher.enrich(document);

    var summary = enrichedDocument.getMetadata().get("file_summary");
    assertTrue(summary instanceof String);
  }

  @Test
  void testStringQueryEmbedding() {
    float[] embedding = this.embeddingModel.embed("Some query");
    assertEquals(properties.getOptions().getDimensions(), embedding.length);
  }

  @Test
  void testRequestToJina() {
    EmbeddingResponse response = embeddingModel.embedForResponse(List.of("Coming back at you with a doozy."));

    assertEquals(1, response.getResults().size());
    assertEquals(1024, response.getResults().get(0).getOutput().length);
  }

  @Test
  void testBatchingRequestsToEmbeddingModel() throws IOException {
    String sampleText = "This is a sample text created for testing purposes. For more dynamic inputs, refer to JinaBatchingStrategyTest";
    Map<String, Object> metadata = Map.of("file_summary", sampleText);

    List<Document> documents = List.of(
        new Document(sampleText, metadata),
        new Document(sampleText, metadata),
        new Document(sampleText, metadata));

    TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

    int documentTokens = tokenCountEstimator.estimate(sampleText);
    int batchTokenLimit = (int) Math.floor((documentTokens * 3) * 1.15);

    List<float[]> embeddings = embeddingModel.embed(
        documents,
        JinaEmbeddingOptions.builder().build(),
        new OverlapBatchingStrategy(batchTokenLimit));

    assertEquals(documents.size(), embeddings.size());
    assertEquals(1024, embeddings.get(0).length);
  }

  @Test
  void testApplicationPropertiesUsedByEmbeddingModel() {
    EmbeddingResponse embeddingResponse = this.embeddingModel
        .call(new EmbeddingRequest(List.of("Hello world"), EmbeddingOptions.builder().build()));

    boolean defaultLateChunking = true;
    assertEquals(!defaultLateChunking, properties.getOptions().isLateChunking());
    assertEquals(properties.getOptions().getModel(), embeddingResponse.getMetadata().getModel());
  }

  @Test
  void testPersistAndSearchInVectorStore() {
    String content = "The implementation of the RAG architecture improves chatbot accuracy significantly.";
    Map<String, Object> metadata = Map.of(
        "author", "DevTeam",
        "topic", "AI");

    Document document = new Document(content, metadata);

    vectorStore.add(List.of(document));

    List<Document> results = vectorStore.similaritySearch(
        SearchRequest.builder().query("enhancing conversational agents")
            .topK(1)
            .similarityThreshold(0.5)
            .build());

    assertEquals(1, results.size(), "Should find exactly one result");

    Document foundDocument = results.get(0);
    assertEquals(content, foundDocument.getText(), "Content should match preserved text");

    assertEquals("DevTeam", foundDocument.getMetadata().get("author"), "Metadata 'author' should be preserved");
    assertEquals("AI", foundDocument.getMetadata().get("topic"), "Metadata 'topic' should be preserved");
  }
}
