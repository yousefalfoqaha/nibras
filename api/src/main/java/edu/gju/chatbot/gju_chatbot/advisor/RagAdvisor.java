package edu.gju.chatbot.gju_chatbot.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class RagAdvisor implements BaseAdvisor {

  private static final PromptTemplate USER_PROMPT_TEMPLATE = new PromptTemplate(
      """
          YOUR IDENTITY:
          You are a helpful AI assistant for the German Jordanian University (GJU) that helps students find information about its study plans (curriculums).
          Your name is GJUBot.

          {query}

          Analyze the provided "Context Documents" to answer the user's question, with no prior knowledge.

          Context Documents (surrounded by ---):

          ---
          {context}
          ---

          Follow these rules:

          1. NEVER mention the context, the documents, your knowledge limitations, or the user's message/query.
          2. If the answer is explicitly found in the context, provide the direct answer.
          3. If the context does not contain the answer, state that you cannot provide the specific information requested, and instead, suggest logical next steps or relevant related topics the user might need, maintaining a helpful tone.
          4. If the question is about you personally, refer to the YOUR IDENTITY section.

          Your response must be a direct answer to the user.
          """);

  private final VectorStoreRetriever documentRetriever;

  private final JdbcTemplate jdbcTemplate;

  private int order = 2;

  public RagAdvisor(VectorStoreRetriever documentRetriever, JdbcTemplate jdbcTemplate) {
    this.documentRetriever = documentRetriever;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
    String query = chatClientRequest.prompt().getUserMessage().getText();
    List<Document> chunks = documentRetriever.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .similarityThreshold(0.4)
            .topK(20)
            .build());

    List<String> sectionIds = chunks.stream()
        .map(doc -> doc.getMetadata().get(DocumentMetadataKeys.SECTION_ID))
        .filter(id -> id != null)
        .map(Object::toString)
        .distinct()
        .toList();

    String queryString = """
          SELECT
            content,
            metadata,
            metadata ->> 'section_id' AS section_id
          FROM vector_store
          GROUP BY section_id
          ORDER BY (metadata ->> 'chunk_index')::int
        """;

    List<Document> sectionDocuments = this.jdbcTemplate.query(
        queryString,
        (resultSet, rowNum) -> {
          Document document = new Document();
        });

    String context = chunks.stream()
        .map(Document::getFormattedContent)
        .collect(Collectors.joining("\n\n"));

    String augmentedQuery = USER_PROMPT_TEMPLATE
        .render(Map.of("query", query, "context", context));

    System.out.println(augmentedQuery);

    return chatClientRequest.mutate()
        .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedQuery))
        .build();
  }

  @Override
  public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
    return chatClientResponse;
  }

  @Override
  public int getOrder() {
    return this.order;
  }
}
