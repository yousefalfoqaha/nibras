package edu.gju.chatbot.gju_chatbot.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

public class IdentityAdvisor implements BaseAdvisor {

  private static final String SYSTEM_TEXT = """
        You are a helpful AI assistant for the German Jordanian University (GJU) that helps students find information about its study plans (curriculums).
        Your name is GJUBot.
      """;

  private int order = 0;

  @Override
  public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
    return chatClientRequest.mutate()
        .prompt(chatClientRequest.prompt().augmentSystemMessage(SYSTEM_TEXT))
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
