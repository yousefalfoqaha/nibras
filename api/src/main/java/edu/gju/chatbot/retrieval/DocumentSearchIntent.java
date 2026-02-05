package edu.gju.chatbot.retrieval;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DocumentSearchIntent {

    private String query;

    private String documentType;

    private Integer targetYear;

    private Map<String, Object> confirmedAttributes;

    private Map<String, List<Object>> unconfirmedAttributes;
}
