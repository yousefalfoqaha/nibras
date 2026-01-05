package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.gju.chatbot.gju_chatbot.transformer.FileSummaryEnricher;
import edu.gju.chatbot.gju_chatbot.transformer.VisualInspectionRefiner;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownHeaderTextSplitter;

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
  public VisualInspectionRefiner visualInspectionRefiner(OpenAiChatModel baseChatModel) {
    OpenAiChatModel visualInspectionModel = baseChatModel.mutate()
        .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").temperature(0.0).build())
        .build();

    return new VisualInspectionRefiner(visualInspectionModel);
  }

  @Bean
  public MarkdownHeaderTextSplitter markdownHeaderTextSplitter() {
    return new MarkdownHeaderTextSplitter();
  }
}
