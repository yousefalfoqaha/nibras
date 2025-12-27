package edu.gju.chatbot.gju_chatbot.jina;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JinaEmbeddingModel implements EmbeddingModel {

  private final RestClient restClient;

  private final RetryTemplate retryTemplate;

  private final MetadataMode metadataMode;

  private final JinaEmbeddingOptions defaultOptions;

  public JinaEmbeddingModel(RestClient restClient, MetadataMode metadataMode, JinaEmbeddingOptions defaultOptions,
      RetryTemplate retryTemplate) {
    Assert.notNull(metadataMode, "metadataMode must not be null");
    Assert.notNull(defaultOptions, "options must not be null");
    Assert.notNull(retryTemplate, "retryTemplate must not be null");

    this.restClient = restClient;
    this.metadataMode = metadataMode;
    this.defaultOptions = defaultOptions;
    this.retryTemplate = retryTemplate;
  }

  public EmbeddingResponse call(EmbeddingRequest request) {
    JinaApiEmbeddingRequest apiRequest = buildApiRequest(request);
    JinaApiEmbeddingResponse apiResponse = retryTemplate.execute(context -> callApi(apiRequest));

    if (apiResponse == null || CollectionUtils.isEmpty(apiResponse.data())) {
      return new EmbeddingResponse(List.of());
    }

    List<Embedding> embeddings = apiResponse.data()
        .stream()
        .sorted(java.util.Comparator.comparingInt(JinaEmbedding::index))
        .map(entry -> new Embedding(entry.embedding(), entry.index()))
        .toList();

    JinaApiUsage usage = apiResponse.usage();
    Usage embeddingResponseUsage = new DefaultUsage(
        usage.promptTokens(),
        0,
        usage.totalTokens());
    var metadata = new EmbeddingResponseMetadata(apiResponse.model(), embeddingResponseUsage);

    return new EmbeddingResponse(embeddings, metadata);
  }

  @Override
  public float[] embed(Document document) {
    Assert.notNull(document, "Document must not be null");
    return this.embed(document.getFormattedContent(this.metadataMode));
  }

  @Override
  public List<float[]> embed(List<String> texts) {
    Assert.notNull(texts, "Texts must not be null");
    return this.call(new EmbeddingRequest(texts, this.defaultOptions))
        .getResults()
        .stream()
        .map(Embedding::getOutput)
        .toList();
  }

  @Override
  public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
    Assert.notNull(documents, "Documents must not be null");

    List<float[]> finalTargetEmbeddings = new ArrayList<>();
    List<List<Document>> batches = batchingStrategy.batch(documents);

    for (List<Document> batch : batches) {
      List<String> texts = batch.stream().map(Document::getText).toList();
      EmbeddingRequest request = new EmbeddingRequest(texts, options);

      EmbeddingResponse response = this.call(request);

      for (int i = 0; i < batch.size(); i++) {
        Document doc = batch.get(i);
        String type = (String) doc.getMetadata().get("chunk_type");

        if ("TARGET".equals(type)) {
          finalTargetEmbeddings.add(response.getResults().get(i).getOutput());
        }
      }
    }

    Assert.isTrue(finalTargetEmbeddings.size() == documents.size(),
        "Expected " + documents.size() + " target embeddings, but got " + finalTargetEmbeddings.size());

    return finalTargetEmbeddings;
  }

  private JinaApiEmbeddingRequest buildApiRequest(EmbeddingRequest embeddingRequest) {
    EmbeddingOptions options = embeddingRequest.getOptions();
    JinaEmbeddingOptions merged = JinaEmbeddingOptions.mergeWithDefaults(options, this.defaultOptions);

    return new JinaApiEmbeddingRequest(
        merged.getModel(),
        merged.getTask().getValue(),
        merged.getDimensions(),
        merged.isLateChunking(),
        embeddingRequest.getInstructions());
  }

  private JinaApiEmbeddingResponse callApi(JinaApiEmbeddingRequest embeddingRequest) {
    Assert.notNull(embeddingRequest, "The request body can not be null.");

    // Input text to embed, encoded as a string or array of tokens. To embed
    // multiple
    // inputs in a single
    // request, pass an array of strings or array of token arrays.
    Assert.notNull(embeddingRequest.input(), "The input can not be null.");
    Assert.isTrue(embeddingRequest.input() instanceof List,
        "The input must be either a String, or a List of Strings or List of List of integers.");

    // The input must not exceed the max input tokens for the model (8192 tokens).
    // Cannot be an empty string, and any array must be 2048 dimensions or less.
    if (embeddingRequest.input() instanceof List<?> list) {
      Assert.isTrue(!CollectionUtils.isEmpty(list), "The input list can not be empty.");
      Assert.isTrue(list.size() <= 2048, "The list must be 2048 dimensions or less");
      Assert.isTrue(
          list.get(0) instanceof String || list.get(0) instanceof Integer || list.get(0) instanceof List,
          "The input must be either a String, or a List of Strings or list of list of integers.");
    }

    return this.restClient.post()
        .body(embeddingRequest)
        .retrieve()
        .toEntity(JinaApiEmbeddingResponse.class)
        .getBody();

  }

  protected record JinaApiEmbeddingRequest(
      @JsonProperty("model") String model,
      @JsonProperty("task") String task,
      @JsonProperty("dimensions") int dimensions,
      @JsonProperty("late_chunking") boolean lateChunking,
      @JsonProperty("input") List<String> input) {
  }

  protected record JinaApiEmbeddingResponse(
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
