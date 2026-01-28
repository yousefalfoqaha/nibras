package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.gju.chatbot.gju_chatbot.tool.KnowledgeBaseTools;

@Configuration
public class ChatClientConfig {

  @Bean
  public ChatClient openAiChatClient(OpenAiChatModel chatModel) {

    return ChatClient.builder(chatModel)
        .defaultTools(new KnowledgeBaseTools())
        .build();
  }
}
