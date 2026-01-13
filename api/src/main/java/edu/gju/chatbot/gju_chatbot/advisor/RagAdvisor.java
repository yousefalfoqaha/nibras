package edu.gju.chatbot.gju_chatbot.advisor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.gju.chatbot.gju_chatbot.exception.RagException;
import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class RagAdvisor implements BaseAdvisor {

  private static final PromptTemplate USER_PROMPT_TEMPLATE = new PromptTemplate(
      """
          YOUR IDENTITY:
          You are a helpful AI assistant for the German Jordanian University (GJU) that helps students find information about its study plans (curriculums).
          Your name is GJUBot.

          OUTPUT FORMAT:
          - Respond using **Markdown**.
          - Use Markdown features such as headings, bullet lists, tables, and block quotes **when they improve clarity or structure**.
          - Do NOT force tables or headings if they do not naturally fit the information.
          - Preserve **all relevant information** found in the context; do not omit details, constraints, notes, or exceptions.

          {query}

          Analyze the provided "Context Documents" to answer the user's question, with no prior knowledge.

          Context Documents (surrounded by ---):

          ---
          {context}
          ---

          RULES:
          1. NEVER mention the context, the documents, your knowledge limitations, or the user's message/query.
          2. If the answer is explicitly found in the context, provide the answer completely and accurately.
          3. If the context does not contain the answer, state that you cannot provide the specific information requested, and instead suggest logical next steps or related topics.
          4. If the question is about you personally, refer to the YOUR IDENTITY section.
          5. Do NOT summarize or compress information unless the context itself is summarized.

          Your response must be a direct answer to the user.
          """);

  private static final Logger log = LoggerFactory.getLogger(RagAdvisor.class);

  private final VectorStoreRetriever documentRetriever;

  private final JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper;

  private final int order = 2;

  public RagAdvisor(VectorStoreRetriever documentRetriever, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.documentRetriever = documentRetriever;
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
    String query = chatClientRequest.prompt().getUserMessage().getText();

    List<Document> initialChunks = documentRetriever.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .similarityThreshold(0.4)
            .topK(3)
            .build());

    List<String> sectionIds = initialChunks.stream()
        .map(doc -> doc.getMetadata().get(DocumentMetadataKeys.SECTION_ID))
        .filter(Objects::nonNull)
        .map(Object::toString)
        .distinct()
        .toList();

    if (sectionIds.isEmpty()) {
      return chatClientRequest;
    }

    String inSql = sectionIds.stream()
        .map(s -> "'" + s + "'")
        .collect(Collectors.joining(","));

    String queryString = String.format("""
        SELECT
            content,
            metadata
        FROM vector_store
        WHERE metadata ->> 'section_id' IN (%s)
        """, inSql);

    List<Document> expandedDocuments = this.jdbcTemplate.query(
        queryString,
        (resultSet, rowNum) -> {
          Map<String, Object> metadata = new HashMap<>();
          try {
            metadata = objectMapper.readValue(resultSet.getString("metadata"),
                new TypeReference<Map<String, Object>>() {
                });
          } catch (Exception e) {
            throw new RagException("Failed to parse document metadata");
          }
          return new Document(resultSet.getString("content"), metadata);
        });

    Map<String, List<Document>> sectionMap = expandedDocuments.stream()
        .collect(Collectors.groupingBy(doc -> doc.getMetadata().get(DocumentMetadataKeys.SECTION_ID).toString()));

    StringBuilder contextBuilder = new StringBuilder();
    String lastFileId = "";
    String lastBreadcrumb = "";

    for (String sectionId : sectionIds) {
      List<Document> sectionChunks = sectionMap.get(sectionId);

      if (sectionChunks == null || sectionChunks.isEmpty())
        continue;

      sectionChunks.sort(Comparator
          .comparingInt(doc -> Integer.parseInt(doc.getMetadata().get(DocumentMetadataKeys.CHUNK_INDEX).toString())));

      for (Document chunk : sectionChunks) {
        Map<String, Object> metadata = chunk.getMetadata();
        String currentFileId = (String) metadata.get(DocumentMetadataKeys.FILE_ID);
        String currentBreadcrumb = (String) metadata.get(DocumentMetadataKeys.BREADCRUMBS);
        String fileSummary = (String) metadata.get(DocumentMetadataKeys.FILE_SUMMARY);

        if (!Objects.equals(currentFileId, lastFileId)) {
          contextBuilder.append("\n\n=== FILE SUMMARY ===\n")
              .append(fileSummary)
              .append("\n====================\n");
          lastFileId = currentFileId;
          lastBreadcrumb = "";
        }

        if (!Objects.equals(currentBreadcrumb, lastBreadcrumb)) {
          contextBuilder.append("\n** Location: ").append(currentBreadcrumb).append(" **\n");
          lastBreadcrumb = currentBreadcrumb;
        }

        contextBuilder.append(chunk.getText()).append("\n");
      }
      contextBuilder.append("\n\n");
    }

    String augmentedQuery = USER_PROMPT_TEMPLATE
        .render(Map.of("query", query, "context", contextBuilder.toString()));

    log.info(augmentedQuery);

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
