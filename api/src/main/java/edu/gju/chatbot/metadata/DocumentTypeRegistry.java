package edu.gju.chatbot.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ResourceLoader;

public class DocumentTypeRegistry {

    private final List<DocumentType> documentTypes;

    private final List<DocumentAttribute> documentAttributes;

    private final ResourceLoader resourceLoader;

    private final ObjectMapper objectMapper;

    private final String yamlPath;

    public DocumentTypeRegistry(
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

    public Optional<DocumentType> getDocumentType(String name) {
        return documentTypes
            .stream()
            .filter(t -> t.getName().equalsIgnoreCase(name))
            .findFirst();
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
        Map<String, DocumentAttribute> attributes = new HashMap<>();

        if (config.attributes != null) {
            for (var entry : config.attributes.entrySet()) {
                String name = entry.getKey();
                AttributeConfig attributeConfig = entry.getValue();

                List<String> values = new ArrayList<>();
                if (attributeConfig.values != null) {
                    for (var v : attributeConfig.values) {
                        values.add(v.toString());
                    }
                }

                DocumentAttribute attribute = new DocumentAttribute(
                    name,
                    attributeConfig.description,
                    values
                );
                attributes.put(name, attribute);
            }
        }

        List<DocumentType> documentTypes = new ArrayList<>();
        if (config.document_types != null) {
            for (var entry : config.document_types.entrySet()) {
                String name = entry.getKey();
                DocumentTypeConfig documentTypeConfig = entry.getValue();

                List<DocumentAttribute> requiredAttributes =
                    Optional.ofNullable(documentTypeConfig.attributes())
                        .map(a -> a.required())
                        .orElse(List.of())
                        .stream()
                        .map(a -> {
                            DocumentAttribute resolved = attributes.get(a);
                            if (resolved == null) throw new RagException(
                                String.format(
                                    "Document type {} with unknown required attribute {}",
                                    name,
                                    a
                                )
                            );
                            return resolved;
                        })
                        .toList();

                List<DocumentAttribute> optionalAttributes =
                    Optional.ofNullable(documentTypeConfig.attributes())
                        .map(a -> a.optional())
                        .orElse(List.of())
                        .stream()
                        .map(a -> {
                            DocumentAttribute resolved = attributes.get(a);
                            if (resolved == null) throw new RagException(
                                String.format(
                                    "Document type {} with unknown optional attribute {}",
                                    name,
                                    a
                                )
                            );
                            return resolved;
                        })
                        .toList();

                documentTypes.add(
                    new DocumentType(
                        name,
                        documentTypeConfig.description,
                        Optional.ofNullable(
                            documentTypeConfig.requires_year()
                        ).orElse(false),
                        Optional.ofNullable(
                            documentTypeConfig.prefer_latest_year()
                        ).orElse(false),
                        requiredAttributes,
                        optionalAttributes
                    )
                );
            }
        }

        return new RegistryData(
            List.copyOf(documentTypes),
            List.copyOf(attributes.values())
        );
    }

    private record RegistryData(
        List<DocumentType> documentTypes,
        List<DocumentAttribute> documentAttributes
    ) {}

    private record MetadataConfig(
        Map<String, DocumentTypeConfig> document_types,
        Map<String, AttributeConfig> attributes
    ) {}

    private record AttributeConfig(String description, List<Object> values) {}

    private record DocumentTypeConfig(
        String description,
        Boolean requires_year,
        Boolean prefer_latest_year,
        AttributesConfig attributes
    ) {}

    private record AttributesConfig(
        List<String> required,
        List<String> optional
    ) {}
}
