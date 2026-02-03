package edu.gju.chatbot.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.core.io.ResourceLoader;

public class DocumentMetadataRegistry {

    private final List<DocumentType> documentTypes;

    private final List<DocumentAttribute> documentAttributes;

    private final ResourceLoader resourceLoader;

    private final ObjectMapper objectMapper;

    private final String yamlPath;

    public DocumentMetadataRegistry(
        ResourceLoader resourceLoader,
        ObjectMapper objectMapper,
        String yamlPath
    ) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.yamlPath = yamlPath;

        MetadataConfig config = loadConfig();
        RegistryData data = buildRegistry(config);

        this.documentTypes = data.documentTypes();
        this.documentAttributes = data.documentAttributes();
    }

    public List<DocumentType> getDocumentTypes() {
        return documentTypes;
    }

    public List<DocumentAttribute> getDocumentAttributes() {
        return documentAttributes;
    }

    private MetadataConfig loadConfig() {
        try (
            var is = resourceLoader
                .getResource("classpath:" + yamlPath)
                .getInputStream()
        ) {
            return objectMapper.readValue(is, MetadataConfig.class);
        } catch (IOException e) {
            throw new RagException(
                "Failed to load document metadata from " + yamlPath
            );
        }
    }

    private RegistryData buildRegistry(MetadataConfig config) {
        Map<String, DocumentAttribute> attributeMap = new HashMap<>();
        List<DocumentAttribute> attributes = new ArrayList<>();

        if (config.attributes != null) {
            for (var entry : config.attributes.entrySet()) {
                String name = entry.getKey();
                AttributeConfig attributeConfig = entry.getValue();

                DocumentAttribute.AttributeType type =
                    DocumentAttribute.AttributeType.STRING;
                if (attributeConfig.type != null) {
                    switch (attributeConfig.type.toLowerCase()) {
                        case "integer" -> type =
                            DocumentAttribute.AttributeType.INTEGER;
                        case "string" -> type =
                            DocumentAttribute.AttributeType.STRING;
                        default -> throw new RagException(
                            "Invalid type for attribute '" +
                                name +
                                "': " +
                                attributeConfig.type
                        );
                    }
                }

                List<String> values = new ArrayList<>();
                if (attributeConfig.values != null) {
                    for (var v : attributeConfig.values) {
                        if (type == DocumentAttribute.AttributeType.INTEGER) {
                            try {
                                Integer.parseInt(v.toString());
                                values.add(v.toString());
                            } catch (NumberFormatException e) {
                                throw new RagException(
                                    "Attribute '" +
                                        name +
                                        "' has non-integer value: " +
                                        v
                                );
                            }
                        } else {
                            values.add(v.toString());
                        }
                    }
                }

                DocumentAttribute attribute = new DocumentAttribute(
                    name,
                    attributeConfig.description,
                    type,
                    values
                );
                attributeMap.put(name, attribute);
                attributes.add(attribute);
            }
        }

        List<DocumentType> documentTypes = new ArrayList<>();
        if (config.document_types != null) {
            for (var entry : config.document_types.entrySet()) {
                String name = entry.getKey();
                DocumentTypeConfig documentTypeConfig = entry.getValue();

                List<String> required = resolveAttributes(
                    name,
                    documentTypeConfig.attributes != null
                        ? documentTypeConfig.attributes.required
                        : null,
                    attributeMap
                );

                List<String> optional = resolveAttributes(
                    name,
                    documentTypeConfig.attributes != null
                        ? documentTypeConfig.attributes.optional
                        : null,
                    attributeMap
                );

                Set<String> intersection = new HashSet<>(required);
                intersection.retainAll(optional);
                if (!intersection.isEmpty()) {
                    throw new RagException(
                        "Document type '" +
                            name +
                            "' has attributes both required and optional: " +
                            intersection
                    );
                }

                documentTypes.add(
                    new DocumentType(
                        name,
                        documentTypeConfig.description,
                        required,
                        optional
                    )
                );
            }
        }

        return new RegistryData(
            List.copyOf(documentTypes),
            List.copyOf(attributes)
        );
    }

    private List<String> resolveAttributes(
        String documentType,
        List<String> names,
        Map<String, DocumentAttribute> attributeMap
    ) {
        if (names == null) return List.of();

        return names
            .stream()
            .map(a -> {
                if (!attributeMap.containsKey(a)) {
                    throw new RagException(
                        "Document type '" +
                            documentType +
                            "' references unknown attribute '" +
                            a +
                            "'"
                    );
                }
                return a;
            })
            .collect(Collectors.toList());
    }

    private record RegistryData(
        List<DocumentType> documentTypes,
        List<DocumentAttribute> documentAttributes
    ) {}

    private record MetadataConfig(
        Map<String, AttributeConfig> attributes,
        Map<String, DocumentTypeConfig> document_types
    ) {}

    private record AttributeConfig(
        String description,
        String type,
        List<Object> values
    ) {}

    private record DocumentTypeConfig(
        String description,
        AttributesConfig attributes
    ) {}

    private record AttributesConfig(
        List<String> required,
        List<String> optional
    ) {}
}
