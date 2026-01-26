package edu.gju.chatbot.gju_chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.gju.chatbot.gju_chatbot.metadata.MetadataFilterRepository;
import edu.gju.chatbot.gju_chatbot.transformer.FileMetadataEnricher;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownHierarchyEnricher;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownTextSplitter;

@Configuration
public class EtlPipelineConfig {

  @Bean
  public MarkdownHierarchyEnricher markdownHierarchyEnricher(OpenAiChatModel chatModel) {
    System.out.println("Markdown hierarchy chat client");
    System.out.println(chatModel.getDefaultOptions().toString());

    ChatClient chatClient = ChatClient.builder(chatModel)
        .defaultOptions(OpenAiChatOptions.builder().model("gpt-5.2").build())
        .build();

    return new MarkdownHierarchyEnricher(chatClient);
  }

  @Bean
  @ConditionalOnMissingBean(TextSplitter.class)
  public TextSplitter textSplitter() {
    return new TokenTextSplitter(
        128,
        64,
        10,
        5000,
        true);
  }

  @Bean
  public MarkdownTextSplitter markdownHeaderTextSplitter(TextSplitter textSplitter) {
    return new MarkdownTextSplitter(textSplitter);
  }

  @Bean
  public FileMetadataEnricher fileSummaryEnricher(OpenAiChatModel chatModel,
      MetadataFilterRepository metadataFilterRepository) {
    System.out.println("Chat client straight from application context:");

    ChatClient chatClient = ChatClient.builder(chatModel)
        .defaultOptions(OpenAiChatOptions.builder().model("gpt-5-mini").build())
        .build();

    return new FileMetadataEnricher(chatClient, metadataFilterRepository);
  }
}
