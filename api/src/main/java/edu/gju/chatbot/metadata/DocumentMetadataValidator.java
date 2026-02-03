package edu.gju.chatbot.metadata;

import edu.gju.chatbot.exception.RagException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DocumentMetadataValidator {

    private final DocumentMetadataRegistry registry;

    public DocumentType validateDocumentType(String type) {
        return registry
            .getDocumentTypes()
            .stream()
            .filter(t -> t.getName().equals(type))
            .findFirst()
            .orElseThrow(() ->
                new RagException("Document type '" + type + "' does not exist")
            );
    }

    public void validateDocumentAttributes(Map<String, Object> attributes) {
        Map<String, DocumentAttribute> registryMap = registry
            .getDocumentAttributes()
            .stream()
            .collect(Collectors.toMap(DocumentAttribute::getName, a -> a));

        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            String attrName = e.getKey();
            Object value = e.getValue();

            DocumentAttribute a = registryMap.get(attrName);
            if (a == null) {
                throw new RagException(
                    "Attribute '" +
                        attrName +
                        "' is not defined in the metadata schema"
                );
            }

            validateAttributeValue(a, value);
        }
    }

    public List<String> getMissingDocumentTypeAttributes(
        Map<String, Object> attributes,
        String documentType
    ) {
        DocumentType typeReference = validateDocumentType(documentType);

        if (
            typeReference.getRequiredAttributes() == null ||
            typeReference.getRequiredAttributes().isEmpty()
        ) {
            return List.of();
        }

        return typeReference
            .getRequiredAttributes()
            .stream()
            .filter(requiredAttr -> !attributes.containsKey(requiredAttr))
            .collect(Collectors.toList());
    }

    private void validateAttributeValue(
        DocumentAttribute attribute,
        Object value
    ) {
        if (value == null) {
            throw new RagException(
                "Attribute '" +
                    attribute.getName() +
                    "' must have a non-null value"
            );
        }

        switch (attribute.getType()) {
            case STRING -> validateStringAttribute(attribute, value);
            case INTEGER -> validateIntegerAttribute(attribute, value);
        }
    }

    private void validateStringAttribute(
        DocumentAttribute attribute,
        Object value
    ) {
        String strValue = value.toString();
        if (
            !attribute.getValues().isEmpty() &&
            !attribute.getValues().contains(strValue)
        ) {
            throw new RagException(
                "Invalid value for attribute '" +
                    attribute.getName() +
                    "': '" +
                    strValue +
                    "'. Allowed values: " +
                    attribute.getValues()
            );
        }
    }

    private void validateIntegerAttribute(
        DocumentAttribute attribute,
        Object value
    ) {
        int intValue = parseInteger(value, attribute.getName());

        if (!attribute.getValues().isEmpty()) {
            List<Integer> allowed = attribute
                .getValues()
                .stream()
                .map(v -> Integer.parseInt(v.toString()))
                .toList();
            if (!allowed.contains(intValue)) {
                throw new RagException(
                    "Invalid value for attribute '" +
                        attribute.getName() +
                        "': " +
                        intValue +
                        ". Allowed values: " +
                        allowed
                );
            }
        }
    }

    private int parseInteger(Object value, String attributeName) {
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new RagException(
                "Attribute '" +
                    attributeName +
                    "' must be an integer, got: " +
                    value
            );
        }
    }
}
