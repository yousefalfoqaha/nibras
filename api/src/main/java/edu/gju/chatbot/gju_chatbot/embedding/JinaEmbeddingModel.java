package edu.gju.chatbot.gju_chatbot.embedding;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.gju.chatbot.gju_chatbot.exception.FileProcessingException;

public class JinaEmbeddingModel implements EmbeddingModel {

  private static final Logger log = LoggerFactory.getLogger(JinaEmbeddingModel.class);

  private final RestClient restClient;

  private final RetryTemplate retryTemplate;

  private final MetadataMode metadataMode;

  private final JinaEmbeddingOptions defaultOptions;

  public JinaEmbeddingModel(RestClient restClient, MetadataMode metadataMode,
      JinaEmbeddingOptions defaultOptions, RetryTemplate retryTemplate) {
    Assert.notNull(metadataMode, "metadataMode must not be null");
    Assert.notNull(defaultOptions, "options must not be null");
    Assert.notNull(retryTemplate, "retryTemplate must not be null");

    this.restClient = restClient;
    this.metadataMode = metadataMode;
    this.defaultOptions = defaultOptions;
    this.retryTemplate = retryTemplate;

    log.info("JinaEmbeddingModel initialized with model: {}", defaultOptions.getModel());
  }

  public EmbeddingResponse call(EmbeddingRequest request) {
    JinaApiEmbeddingRequest apiRequest = buildApiRequest(request);
    log.debug("Built API request: {}", apiRequest);

    JinaApiEmbeddingResponse apiResponse = retryTemplate.execute(context -> {
      log.info("Calling Jina AI to embed {} chunks...", request.getInstructions().size());
      return callApi(apiRequest);
    });

    if (apiResponse == null || CollectionUtils.isEmpty(apiResponse.data())) {
      log.warn("API response is empty or null");
      return new EmbeddingResponse(List.of());
    }

    List<Embedding> embeddings = apiResponse.data()
        .stream()
        .sorted(java.util.Comparator.comparingInt(JinaEmbedding::index))
        .map(entry -> new Embedding(entry.embedding(), entry.index()))
        .toList();

    log.info("Received {} chunk embeddings from Jina AI", embeddings.size());

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
    log.debug("Embedding document with metadata: {}", document.getMetadata());
    Assert.notNull(document, "Document must not be null");
    return this.embed(document.getFormattedContent(this.metadataMode));
  }

  @Override
  public List<float[]> embed(List<String> texts) {
    log.debug("Embedding {} texts", texts.size());
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

    log.info("Received {} chunks in {} batches", documents.size(), batches.size());

    for (List<Document> batch : batches) {
      List<String> texts = batch.stream().map(Document::getText).toList();
      EmbeddingRequest request = new EmbeddingRequest(texts, options);

      EmbeddingResponse response = this.call(request);

      if (response.getResults().size() != batch.size()) {
        throw new FileProcessingException("Embeddings returned do not match batch size");
      }

      for (int i = 0; i < batch.size(); i++) {
        Document doc = batch.get(i);
        String type = (String) doc.getMetadata().get("chunk_type");

        if ("TARGET".equals(type)) {
          finalTargetEmbeddings.add(response.getResults().get(i).getOutput());
          log.info("Target chunk found at index {} in batch", i);
        } else {
          log.info("{} chunk ignored", type);
        }
      }
    }

    Assert.isTrue(finalTargetEmbeddings.size() == documents.size(),
        "Expected " + documents.size() + " target embeddings, but got " + finalTargetEmbeddings.size());

    log.info("Finished embedding. Total target embeddings: {}", finalTargetEmbeddings.size());

    return finalTargetEmbeddings;
  }

  private JinaApiEmbeddingRequest buildApiRequest(EmbeddingRequest embeddingRequest) {
    EmbeddingOptions options = embeddingRequest.getOptions();
    JinaEmbeddingOptions merged = JinaEmbeddingOptions.mergeWithDefaults(options, this.defaultOptions);

    return new JinaApiEmbeddingRequest(
        merged.getModel(),
        merged.getTask() == JinaEmbeddingOptions.Task.NONE ? null : merged.getTask().getValue(),
        merged.getDimensions(),
        merged.isLateChunking(),
        embeddingRequest.getInstructions());
  }

  private JinaApiEmbeddingResponse callApi(JinaApiEmbeddingRequest embeddingRequest) {
    Assert.notNull(embeddingRequest, "The request body cannot be null.");
    Assert.notNull(embeddingRequest.input(), "The input cannot be null.");
    Assert.isTrue(embeddingRequest.input() instanceof List,
        "The input must be either a String, or a List of Strings or List of List of integers.");

    if (embeddingRequest.input() instanceof List<?> list) {
      Assert.isTrue(!CollectionUtils.isEmpty(list), "The input list cannot be empty.");
      Assert.isTrue(list.size() <= 2048, "The list must be 2048 elements or less");
      Assert.isTrue(
          list.get(0) instanceof String || list.get(0) instanceof Integer || list.get(0) instanceof List,
          "The input must be either a String, or a List of Strings or list of list of integers.");
    }

    log.debug("Sending API request with {} items", embeddingRequest.input().size());

    JinaApiEmbeddingResponse response = this.restClient.post()
        .body(embeddingRequest)
        .retrieve()
        .toEntity(JinaApiEmbeddingResponse.class)
        .getBody();

    log.debug("Received API response: model={}, data size={}",
        response != null ? response.model() : "null",
        response != null && response.data() != null ? response.data().size() : 0);

    return response;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
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
