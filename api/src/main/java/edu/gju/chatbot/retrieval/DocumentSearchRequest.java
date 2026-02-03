package edu.gju.chatbot.retrieval;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class DocumentSearchRequest {

    private String query;

    private String documentType;

    private Integer year;

    private Map<String, Object> attributes;
}
