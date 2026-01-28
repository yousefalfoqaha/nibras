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
public class DocumentAttribute {

    private String name;

    private String description;

    private AttributeType type;

    private List<String> values = new ArrayList<>();

    public enum AttributeType {
        INTEGER,
        STRING,
    }

    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(name);

        if (description != null && !description.isEmpty()) {
            sb.append(": ").append(description);
        }

        sb.append(" (").append(type.toString().toLowerCase()).append(")");

        if (!values.isEmpty()) {
            sb.append("\n  Allowed values: ").append(String.join(", ", values));
        }

        return sb.toString();
    }
}
