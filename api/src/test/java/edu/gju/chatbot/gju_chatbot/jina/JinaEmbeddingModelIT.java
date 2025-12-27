package edu.gju.chatbot.gju_chatbot.jina;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.gju.chatbot.gju_chatbot.config.jina.JinaEmbeddingProperties;

@SpringBootTest(properties = { "spring.ai.jina.embedding.options.late-chunking=false" })
// to test if properties change embedding output
@EnabledIfEnvironmentVariable(named = "JINA_API_KEY", matches = ".+")
class JinaEmbeddingModelIT {

  @Autowired
  private JinaEmbeddingModel embeddingModel;

  @Autowired
  JinaEmbeddingProperties properties;

  @Test
  void testRequestToJina() {
    EmbeddingResponse response = embeddingModel.embedForResponse(List.of("Coming back at you with a doozy."));

    assertEquals(1, response.getResults().size());
    assertEquals(1024, response.getResults().get(0).getOutput().length);
  }

  @Test
  void testBatchingRequestsToJina() throws IOException {
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
        new JinaBatchingStrategy(batchTokenLimit));

    assertEquals(documents.size(), embeddings.size());
    assertEquals(1024, embeddings.get(0).length);
  }

  @Test
  void testApplicationPropertiesUsedByModel() {
    EmbeddingResponse embeddingResponse = this.embeddingModel
        .call(new EmbeddingRequest(List.of("Hello world"), EmbeddingOptions.builder().build()));

    boolean defaultLateChunking = true;
    assertEquals(!defaultLateChunking, properties.getOptions().isLateChunking());
    assertEquals(properties.getOptions().getModel(), embeddingResponse.getMetadata().getModel());
  }
}
