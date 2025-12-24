package edu.gju.chatbot.gju_chatbot.jina;

import org.springframework.ai.embedding.EmbeddingOptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class JinaEmbeddingOptions implements EmbeddingOptions {

  private @JsonProperty("model") String model;

  private @JsonProperty("dimensions") Integer dimensions;

  private @JsonProperty("task") Task task;

  private @JsonProperty("late_chunking") boolean lateChunking;

  public enum Task {
    RETRIEVAL_QUERY("retrieval.query"),
    RETRIEVAL_PASSAGE("retrieval.passage"),
    SEPARATION("separation"),
    CLASSIFICATION("classification"),
    TEXT_MATCHING("text-matching"),
    NONE("none");

    private final String value;

    Task(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }
  }
}
