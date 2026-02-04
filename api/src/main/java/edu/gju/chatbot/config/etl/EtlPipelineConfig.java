package edu.gju.chatbot.config.etl;

import com.knuddels.jtokkit.api.EncodingType;
import edu.gju.chatbot.etl.FileMetadataEnricher;
import edu.gju.chatbot.etl.MarkdownHierarchyEnricher;
import edu.gju.chatbot.etl.MarkdownTextSplitter;
import edu.gju.chatbot.metadata.DocumentTypeRegistry;
import edu.gju.chatbot.metadata.MetadataKeys;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtlPipelineConfig {

    @Bean
    public MarkdownHierarchyEnricher markdownHierarchyEnricher(
        OpenAiChatModel chatModel
    ) {
        ChatClient chatClient = ChatClient.builder(chatModel)
            .defaultOptions(
                OpenAiChatOptions.builder().model("gpt-5.2").build()
            )
            .build();

        return new MarkdownHierarchyEnricher(chatClient);
    }

    @Bean
    @ConditionalOnMissingBean(TextSplitter.class)
    public TextSplitter textSplitter() {
        return new TokenTextSplitter(128, 64, 10, 5000, true);
    }

    @Bean
    public MarkdownTextSplitter markdownHeaderTextSplitter(
        TextSplitter textSplitter
    ) {
        return new MarkdownTextSplitter(textSplitter);
    }

    @Bean
    public FileMetadataEnricher fileSummaryEnricher(
        OpenAiChatModel chatModel,
        DocumentTypeRegistry documentTypeRegistry
    ) {
        ChatClient chatClient = ChatClient.builder(chatModel)
            .defaultOptions(
                OpenAiChatOptions.builder().model("gpt-5-mini").build()
            )
            .build();

        return new FileMetadataEnricher(chatClient, documentTypeRegistry);
    }

    @Bean
    public BatchingStrategy tokenCountBatchingStrategy() {
        return new TokenCountBatchingStrategy(
            EncodingType.CL100K_BASE,
            8191,
            0.10,
            DefaultContentFormatter.builder()
                .withExcludedEmbedMetadataKeys(
                    List.of(
                        MetadataKeys.FILE_ID,
                        MetadataKeys.FILE_NAME,
                        MetadataKeys.FILE_SIZE,
                        MetadataKeys.TITLE,
                        MetadataKeys.BREADCRUMBS,
                        MetadataKeys.SECTION_ID,
                        MetadataKeys.CHUNK_INDEX,
                        MetadataKeys.DOCUMENT_TYPE,
                        MetadataKeys.ACADEMIC_LEVEL,
                        MetadataKeys.DEPARTMENT,
                        MetadataKeys.PROGRAM,
                        MetadataKeys.YEAR,
                        "total_chunks",
                        "parent_document_id"
                    )
                )
                .build(),
            MetadataMode.EMBED
        );
    }
}
