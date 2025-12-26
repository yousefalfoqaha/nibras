package edu.gju.chatbot.gju_chatbot.jina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingModel.JinaApiEmbeddingResponse;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingModel.JinaApiUsage;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingModel.JinaEmbedding;
import edu.gju.chatbot.gju_chatbot.jina.JinaEmbeddingOptions.Task;

class JinaEmbeddingModelTest {

  private JinaEmbeddingModel embeddingModel;

  private MockRestServiceServer mockServer;

  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    this.mockServer = MockRestServiceServer.bindTo(builder).build();

    JinaEmbeddingOptions options = JinaEmbeddingOptions.builder()
        .model("jina-embeddings-v3")
        .dimensions(3)
        .task(Task.TEXT_MATCHING)
        .lateChunking(true)
        .build();

    this.embeddingModel = new JinaEmbeddingModel(
        builder.build(),
        MetadataMode.EMBED,
        options,
        new RetryTemplate());
  }

  @Test
  void call_ShouldReturnEmbeddingResponse() throws JsonProcessingException {
    String inputText = "Hello World";
    EmbeddingRequest request = new EmbeddingRequest(List.of(inputText), JinaEmbeddingOptions.builder().build());

    float[] vector = { 0.1f, 0.2f, 0.3f };
    JinaApiEmbeddingResponse apiResponse = new JinaApiEmbeddingResponse(
        "jina-v3", "list", new JinaApiUsage(10, 5),
        List.of(new JinaEmbedding("embedding", 0, vector)));

    mockServer.expect(requestTo("/"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(objectMapper.writeValueAsString(apiResponse), MediaType.APPLICATION_JSON));

    EmbeddingResponse response = embeddingModel.call(request);

    assertThat(response).isNotNull();
    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getResults().get(0).getOutput()).containsExactly(0.1f, 0.2f, 0.3f);

    mockServer.verify();
  }

  @Test
  void embed_ShouldFilterOutNonTargetChunks() throws JsonProcessingException {
    Document userDoc = new Document("User Content");
    List<Document> inputDocs = List.of(userDoc);

    BatchingStrategy mockStrategy = docs -> {
      Document summary = new Document("Summary", Map.of("chunk_type", "FILE_SUMMARY"));
      Document overlap = new Document("Overlap", Map.of("chunk_type", "OVERLAP"));
      Document target = new Document("User Content", Map.of("chunk_type", "TARGET"));

      return List.of(List.of(summary, overlap, target));
    };

    float[] vecSummary = { 0.9f };
    float[] vecOverlap = { 0.8f };
    float[] vecTarget = { 0.1f };

    JinaApiEmbeddingResponse apiResponse = new JinaApiEmbeddingResponse(
        "jina-v3", "list", new JinaApiUsage(30, 10),
        List.of(
            new JinaEmbedding("embedding", 0, vecSummary),
            new JinaEmbedding("embedding", 1, vecOverlap),
            new JinaEmbedding("embedding", 2, vecTarget)));

    mockServer.expect(method(HttpMethod.POST))
        .andRespond(withSuccess(objectMapper.writeValueAsString(apiResponse), MediaType.APPLICATION_JSON));

    List<float[]> results = embeddingModel.embed(inputDocs, JinaEmbeddingOptions.builder().build(), mockStrategy);

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).containsExactly(vecTarget);

    mockServer.verify();
  }
}
