package edu.gju.chatbot.gju_chatbot.jina;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;

public class JinaEmbeddingModel implements EmbeddingModel {

  @Override
  List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {

  }
}
