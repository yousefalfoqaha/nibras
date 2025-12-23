package edu.gju.chatbot.gju_chatbot.jina;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

public class JinaEmbeddingModel implements EmbeddingModel {

  private final RestClient restClient;

  public JinaEmbeddingModel(RestClient restClient) {
    this.restClient = restClient;
  }

  public EmbeddingResponse call(EmbeddingRequest request) {
    return new EmbeddingResponse(List.of());
  }

  public float[] embed(Document document) {
    return [];
  }

  public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
    Assert.notNull(documents, "Documents must not be null");

    List<float[]> embeddings = new ArrayList<>(documents.size());
    List<List<Document>> batches = batchingStrategy.batch(documents);

    for (List<Document> batch : batches) {
      List<String> texts = batch.stream().map(Document::getText).toList();
      EmbeddingRequest request = new EmbeddingRequest(texts, options);

      EmbeddingResponse response = this.call(request);

      for (int i = 0; i < batch.size(); i++) {
        embeddings.add(response.getResults().get(i).getOutput());
      }
    }

    Assert.isTrue(embeddings.size() == documents.size(),
        "Embeddings must have the same number as that of the documents");

    return embeddings;
  }

  protected record JinaApiRequest(
      String model,
      String task,
      boolean lateChunking,
      String[] input) {
  }

  protected record JinaApiResponse(
      String model,
      String list,
      JinaApiUsage usage,
      List<JinaEmbedding> data) {
  }

  protected record JinaApiUsage(
      int totalTokens,
      int promptTokens) {
  }

  protected record JinaEmbedding(
      String object,
      int index,
      float[] embedding) {
  }
}
