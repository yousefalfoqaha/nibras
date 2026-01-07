package edu.gju.chatbot.gju_chatbot.advisor;

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

  private int order = 3;

  public RagAdvisor(VectorStoreRetriever documentRetriever) {
    this.documentRetriever = documentRetriever;
  }

  @Override
  public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
    String query = chatClientRequest.prompt().getUserMessage().getText();
    List<Document> documents = documentRetriever.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .similarityThreshold(0.4)
            .topK(5)
            .build());

    String context = documents.stream()
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
