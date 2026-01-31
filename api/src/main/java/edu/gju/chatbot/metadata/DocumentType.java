package edu.gju.chatbot.metadata;

import java.util.ArrayList;
import java.util.List;
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

    private List<String> requiredAttributes = new ArrayList<>();

    private List<String> optionalAttributes = new ArrayList<>();

    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(name).append(": ").append(description);

        List<String> allAttributes = new ArrayList<>();
        if (requiredAttributes != null) {
            allAttributes.addAll(requiredAttributes);
        }
        if (optionalAttributes != null) {
            allAttributes.addAll(optionalAttributes);
        }

        if (!allAttributes.isEmpty()) {
            sb
                .append("\n  attributes: ")
                .append(String.join(", ", allAttributes));
        }

        return sb.toString();
    }
}
