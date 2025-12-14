package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.gju.chatbot.gju_chatbot.advisor.IdentityAdvisor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class ChatClientConfig {
  private final VectorStore vectorStore;

  @Bean
  public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
    return ChatClient.builder(chatModel)
        .defaultAdvisors(
            // QuestionAnswerAdvisor.builder(vectorStore)
            // .searchRequest(
            // SearchRequest.builder()
            // .similarityThreshold(0.65)
            // .topK(5)
            // .build())
            // .build()
            new IdentityAdvisor())
        .build();
  }
}
