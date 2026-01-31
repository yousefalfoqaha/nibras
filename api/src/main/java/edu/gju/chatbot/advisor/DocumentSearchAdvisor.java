package edu.gju.chatbot.advisor;

import edu.gju.chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.metadata.DocumentType;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;

@RequiredArgsConstructor
public class DocumentSearchAdvisor implements BaseAdvisor {

    private static final String SYSTEM_MESSAGE_TEMPLATE = """
            DOCUMENT TYPES:
            {document_types}

            Decide the document type to search for.

            Extract all required attribute filters for the document type explicitly mentioned in the conversation.

            Only search documents if all the required filters are present.

            If the document type requires a year, and no year was mentioned, default to 2026, and go back a year, and find documents, stop after three years of no documents.

            Do not mention filters in the response.
        """;

    private final DocumentMetadataRegistry documentMetadataRegistry;

    private final int order = 2;

    @Override
    public ChatClientRequest before(
        ChatClientRequest chatClientRequest,
        AdvisorChain advisorChain
    ) {
        String documentTypesDescriptions = documentMetadataRegistry
            .getDocumentTypes()
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n"));

        String systemMessage = PromptTemplate.builder()
            .template(SYSTEM_MESSAGE_TEMPLATE)
            .variables(Map.of("document_types", documentTypesDescriptions))
            .build()
            .render();

        return chatClientRequest
            .mutate()
            .prompt(
                chatClientRequest.prompt().augmentSystemMessage(systemMessage)
            )
            .build();
    }

    @Override
    public ChatClientResponse after(
        ChatClientResponse chatClientResponse,
        AdvisorChain advisorChain
    ) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
