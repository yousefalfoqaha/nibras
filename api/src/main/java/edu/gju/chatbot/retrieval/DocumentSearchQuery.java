package edu.gju.chatbot.retrieval;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DocumentSearchQuery {

    private String query;

    private String documentType;

    private Map<String, Object> attributes;
}
