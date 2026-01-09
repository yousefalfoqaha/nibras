package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.gju.chatbot.gju_chatbot.transformer.FileSummaryEnricher;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownHierarchyEnricher;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownTextSplitter;

@Configuration
public class TransformerConfig {

  @Bean
  public FileSummaryEnricher fileSummaryEnricher(ChatModel chatModel) {
    return new FileSummaryEnricher(chatModel);
  }

  @Bean
  public TokenTextSplitter tokenTextSplitter() {
    return new TokenTextSplitter(512, 218, 10, 5000, true);
  }

  @Bean
  public MarkdownHierarchyEnricher visualInspectionRefiner(OpenAiChatModel chatModel) {
    ChatClient chatClient = ChatClient.builder(chatModel)
        .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.0).build())
        .build();

    return new MarkdownHierarchyEnricher(chatClient);
  }

  @Bean
  public MarkdownTextSplitter markdownHeaderTextSplitter() {
    return new MarkdownTextSplitter();
  }
}
