package edu.gju.chatbot.gju_chatbot.tool;

import edu.gju.chatbot.gju_chatbot.metadata.DocumentAttribute;
import edu.gju.chatbot.gju_chatbot.metadata.DocumentMetadataRegistry;
import edu.gju.chatbot.gju_chatbot.metadata.DocumentType;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

@RequiredArgsConstructor
public class SearchDocumentsTool implements ToolCallback {

    private final DocumentMetadataRegistry documentMetadataRegistry;

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("search_documents")
            .description(buildToolDescription())
            .inputSchema(
                """
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "documentType": {
                      "type": "string"
                    },
                    "attributes": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      }
                    }
                  },
                  "required": ["documentType", "attributes"]
                }
                """
            )
            .build();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().returnDirect(true).build();
    }

    @Override
    public String call(String toolInput) {
        return "";
    }

    private String buildToolDescription() {
        String documentTypesDesc = documentMetadataRegistry
            .getDocumentTypes()
            .stream()
            .map(DocumentType::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        String attributesDesc = documentMetadataRegistry
            .getDocumentAttributes()
            .stream()
            .map(DocumentAttribute::toFormattedString)
            .collect(Collectors.joining("\n\n"));

        return """
        Search for academic documents by type and attributes.

        Once a document type has been selected, only include attributes found for that document type.

        If a required attribute is missing, stop the tool call and ask a clarifying question.

        Available document types:
        %s

        Available attributes:
        %s
        """.formatted(documentTypesDesc, attributesDesc);
    }
}
