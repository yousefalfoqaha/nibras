package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.gju.chatbot.gju_chatbot.advisor.IdentityAdvisor;
import edu.gju.chatbot.gju_chatbot.advisor.RagAdvisor;
import edu.gju.chatbot.gju_chatbot.advisor.RewriteQueryAdvisor;
import edu.gju.chatbot.gju_chatbot.advisor.StudyPlanAdvisor;

@Configuration
public class ChatClientConfig {

  private final VectorStoreRetriever vectorStoreRetriever;

  public ChatClientConfig(VectorStoreRetriever vectorStoreRetriever) {
    this.vectorStoreRetriever = vectorStoreRetriever;
  }

  @Bean
  public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
    ChatClient rewriteClient = ChatClient.builder(chatModel)
        .defaultOptions(OpenAiChatOptions.builder().temperature(0.0).build())
        .build();

    return ChatClient.builder(chatModel)
        .defaultAdvisors(
            new StudyPlanAdvisor(),
            new IdentityAdvisor(),
            new RewriteQueryAdvisor(rewriteClient),
            new RagAdvisor(vectorStoreRetriever))
        .build();
  }
}
