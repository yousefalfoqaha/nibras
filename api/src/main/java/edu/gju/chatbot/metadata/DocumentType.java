package edu.gju.chatbot.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class DocumentType {

    private String name;

    private String description;

    private boolean requiresYear;

    private List<DocumentAttribute> requiredAttributes = new ArrayList<>();

    private List<DocumentAttribute> optionalAttributes = new ArrayList<>();

    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();

        sb.append("- ").append(name).append(": ").append(description);

        sb.append("\n").append("Requires year: ").append(this.requiresYear);

        if (!requiredAttributes.isEmpty()) {
            String attrs = requiredAttributes
                .stream()
                .map(DocumentAttribute::getName)
                .collect(Collectors.joining(", "));

            sb.append("\n").append("Required attributes: ").append(attrs);
        }

        return sb.toString();
    }

    public List<String> getMissingRequiredAttributes(
        Map<String, Object> providedAttributes
    ) {
        return requiredAttributes
            .stream()
            .filter(a -> !providedAttributes.containsKey(a.getName()))
            .map(DocumentAttribute::getName)
            .toList();
    }

    public Map<String, Object> getValidRequiredAttributes(
        Map<String, Object> providedAttributes
    ) {
        return requiredAttributes
            .stream()
            .map(DocumentAttribute::getName)
            .filter(providedAttributes::containsKey)
            .collect(
                Collectors.toMap(Function.identity(), providedAttributes::get)
            );
    }
}
