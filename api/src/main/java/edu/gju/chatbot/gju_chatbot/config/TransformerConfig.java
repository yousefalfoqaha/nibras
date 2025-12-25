package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.gju.chatbot.gju_chatbot.transformer.FileSummaryEnricher;

@Configuration
public class TransformerConfig {

  @Bean
  public FileSummaryEnricher fileSummaryEnricher(ChatModel chatModel) {
    return new FileSummaryEnricher(chatModel);
  }

  @Bean
  public TextSplitter tokenTextSplitter() {
    return new TokenTextSplitter(512, 218, 10, 5000, true);
  }
}
