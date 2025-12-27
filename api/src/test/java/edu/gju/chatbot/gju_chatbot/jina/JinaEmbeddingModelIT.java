package edu.gju.chatbot.gju_chatbot.jina;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "JINA_API_KEY", matches = ".+")
class JinaEmbeddingModelIT {

  @Autowired
  private JinaEmbeddingModel embeddingModel;

  @Autowired
  private BatchingStrategy batchingStrategy;

  @Autowired
  private TokenTextSplitter tokenTextSplitter;

  @Value("classpath:text_source.txt")
  private Resource textSource;

  @Test
  void testRealConnectionToJina() {
    EmbeddingResponse response = embeddingModel.embedForResponse(List.of("Hello Integration Test"));

    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getResults().get(0).getOutput()).hasSize(1024);
  }

  // @Test
  // void testEndToEnd_Split_Batch_And_Embed() throws IOException {
  // String rawText = textSource.getContentAsString(StandardCharsets.UTF_8);
  //
  // Document sourceDoc = new Document(rawText, Map.of(
  // "file_summary", "This is a summary of the heavy text file."));
  //
  // List<Document> chunks = tokenTextSplitter.apply(List.of(sourceDoc));
  //
  // System.out.println("Document split into " + chunks.size() + " chunks.");
  // assertThat(chunks).isNotEmpty();
  //
  // List<float[]> embeddings = embeddingModel.embed(
  // chunks,
  // JinaEmbeddingOptions.builder().build(),
  // this.batchingStrategy);
  //
  // assertThat(embeddings).hasSize(chunks.size());
  // assertThat(embeddings.get(0)).hasSize(1024);
  //
  // System.out.println("Success! End-to-end flow complete. " +
  // chunks.size() + " chunks embedded.");
  // }
}
