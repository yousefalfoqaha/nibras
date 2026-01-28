package edu.gju.chatbot.gju_chatbot.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.gju_chatbot.exception.RagException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ResourceLoader;

public class DocumentMetadataRegistry {

    private final List<DocumentType> documentTypes = new ArrayList<>();

    private final List<DocumentAttribute> documentAttributes =
        new ArrayList<>();

    private ObjectMapper objectMapper;

    private ResourceLoader resourceLoader;

    private String yamlPath;

    public DocumentMetadataRegistry(
        ObjectMapper objectMapper,
        ResourceLoader resourceLoader,
        String yamlPath
    ) {
        this.resourceLoader = resourceLoader;
        this.yamlPath = yamlPath;
        this.objectMapper = objectMapper;

        loadRegistry();
    }

    public List<DocumentType> getDocumentTypes() {
        return this.documentTypes;
    }

    public List<DocumentAttribute> getDocumentAttributes() {
        return this.documentAttributes;
    }

    private void loadRegistry() {
        try {
            JsonNode root = this.objectMapper.readTree(
                resourceLoader
                    .getResource("classpath:" + this.yamlPath)
                    .getInputStream()
            );

            Map<String, DocumentAttribute> attributeMap = new HashMap<>();

            JsonNode attributesNode = root.path("attributes");
            Iterator<String> attributeNames = attributesNode.fieldNames();

            if (attributesNode.isObject()) {
                while (attributeNames.hasNext()) {
                    String attributeName = attributeNames.next();
                    JsonNode attributeData = attributesNode.path(attributeName);

                    String description = attributeData
                        .path("description")
                        .asText();

                    DocumentAttribute.AttributeType type =
                        DocumentAttribute.AttributeType.STRING;

                    if (
                        attributeData
                            .path("type")
                            .asText()
                            .equalsIgnoreCase("integer")
                    ) {
                        type = DocumentAttribute.AttributeType.INTEGER;
                    }

                    List<String> values = new ArrayList<>();
                    for (JsonNode v : attributeData.path("values")) {
                        values.add(v.asText());
                    }

                    DocumentAttribute attribute = new DocumentAttribute(
                        attributeName,
                        description,
                        type,
                        values
                    );

                    attributeMap.put(attributeName, attribute);
                    this.documentAttributes.add(attribute);
                }
            }

            JsonNode documentTypesNode = root.path("document_types");
            Iterator<String> documentTypeNames = documentTypesNode.fieldNames();

            if (documentTypesNode.isObject()) {
                while (documentTypeNames.hasNext()) {
                    String documentTypeName = documentTypeNames.next();
                    JsonNode documentTypeData = documentTypesNode.path(
                        documentTypeName
                    );

                    String description = documentTypeData
                        .path("description")
                        .asText();

                    List<String> requiredAttributes = new ArrayList<>();
                    List<String> optionalAttributes = new ArrayList<>();

                    JsonNode documentTypeAttributes = documentTypeData.path(
                        "attributes"
                    );

                    for (JsonNode attributeNameNode : documentTypeAttributes.path(
                        "required"
                    )) {
                        DocumentAttribute attribute = attributeMap.get(
                            attributeNameNode.asText()
                        );

                        if (attribute != null) {
                            requiredAttributes.add(attribute.getName());
                        }
                    }

                    for (JsonNode attributeNameNode : documentTypeAttributes.path(
                        "optional"
                    )) {
                        DocumentAttribute attribute = attributeMap.get(
                            attributeNameNode.asText()
                        );

                        if (attribute != null) {
                            optionalAttributes.add(attribute.getName());
                        }
                    }

                    DocumentType documentType = new DocumentType(
                        documentTypeName,
                        description,
                        requiredAttributes,
                        optionalAttributes
                    );

                    this.documentTypes.add(documentType);
                }
            }
        } catch (IOException e) {
            throw new RagException("Failed to read document metadata YAML.");
        }
    }
}
