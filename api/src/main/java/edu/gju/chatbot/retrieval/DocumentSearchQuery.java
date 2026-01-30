package edu.gju.chatbot.retrieval;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DocumentSearchQuery {

    private String query;

    private String documentType;

    private Map<String, Object> attributes;
}
