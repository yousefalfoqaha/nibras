package edu.gju.chatbot.gju_chatbot.advisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

public class RewriteQueryAdvisor implements BaseAdvisor {

  private static final SystemPromptTemplate SYSTEM_PROMPT_TEMPLATE = new SystemPromptTemplate("""
      Given a user query, rewrite it to provide better results when querying a vector store.
      Remove any irrelevant information, and ensure the query is concise and specific.

      Original query:
      {query}

      Rewritten query:
      """);

  private int order = 1;

  private final ChatClient chatClient;

  public RewriteQueryAdvisor(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
    String originalQuery = chatClientRequest.prompt().getUserMessage().getText();

    String rewrittenQuery = this.chatClient.prompt()
        .user(user -> user.text(SYSTEM_PROMPT_TEMPLATE.getTemplate())
            .param("query", originalQuery))
        .call()
        .content();

    System.out.println(rewrittenQuery);

    return chatClientRequest.mutate()
        .prompt(chatClientRequest.prompt().augmentUserMessage(rewrittenQuery))
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
