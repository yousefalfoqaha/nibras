package edu.gju.chatbot.gju_chatbot.metadata;

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

        if (!requiredAttributes.isEmpty()) {
            sb
                .append("\n  Required attributes: ")
                .append(String.join(", ", requiredAttributes));
        }

        if (!optionalAttributes.isEmpty()) {
            sb
                .append("\n  Optional attributes: ")
                .append(String.join(", ", optionalAttributes));
        }

        return sb.toString();
    }
}
