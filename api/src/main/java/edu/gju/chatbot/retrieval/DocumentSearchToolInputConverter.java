package edu.gju.chatbot.retrieval;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.gju.chatbot.exception.RagException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentSearchToolInputConverter implements Converter<String, UserQuery> {
    private final ObjectMapper objectMapper;

    @Override
    public UserQuery convert(String toolInput) {
        DocumentSearchToolInput input;
        try {
            input = this.objectMapper.readValue(
                    toolInput,
                    DocumentSearchToolInput.class);
        } catch (IOException e) {
            throw new RagException(
                    "Failed to parse tool input for document searching.",
                    e);
        }

        Map<String, Object> confirmedAttributes = Optional.ofNullable(
                input.conversationAttributes())
                .map(this::parseAndValidateAttributes)
                .orElse(Collections.emptyMap());

        return UserQuery.builder()
                .query(input.query())
                .documentType(input.documentType())
                .targetYear(input.documentTypeYear())
                .confirmedAttributes(confirmedAttributes)
                .build();
    }

    private Map<String, Object> parseAndValidateAttributes(List<String> attributeList) {
        Map<String, List<Object>> attributeGroups = IntStream.range(0, attributeList.size() / 2)
                .boxed()
                .collect(
                        Collectors.groupingBy(
                                i -> attributeList.get(i * 2),
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        i -> (Object) attributeList.get(i * 2 + 1),
                                        Collectors.toList())));

        validateNoDuplicates(attributeGroups);

        return attributeGroups.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().get(0)));
    }

    private void validateNoDuplicates(Map<String, List<Object>> attributeGroups) {
        Map<String, Set<Object>> duplicateAttributes = attributeGroups.entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> new LinkedHashSet<>(entry.getValue()),
                                (a1, a2) -> a1,
                                LinkedHashMap::new));

        if (!duplicateAttributes.isEmpty()) {
            throw new RagException(buildDuplicateAttributeErrorMessage(
                    duplicateAttributes,
                    attributeGroups));
        }
    }

    private String buildDuplicateAttributeErrorMessage(
            Map<String, Set<Object>> duplicateAttributes,
            Map<String, List<Object>> attributeGroups) {

        String duplicateDetails = duplicateAttributes.entrySet()
                .stream()
                .map(entry -> String.format(
                        "  - '%s' appears %d times with values: [%s]",
                        entry.getKey(),
                        attributeGroups.get(entry.getKey()).size(),
                        entry.getValue().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(", "))))
                .collect(Collectors.joining("\n"));

        return String.format(
                "Cannot use the same attribute more than once in a single tool call.\n" +
                        "The following attributes were repeated:\n%s\n\n" +
                        "Please make separate tool calls if you need to search for different values of the same attribute.",
                duplicateDetails);
    }
}
