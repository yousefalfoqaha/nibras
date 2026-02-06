package edu.gju.chatbot.retrieval;

import java.util.List;

import org.springframework.lang.NonNull;

public record DocumentSearchToolInput(

    @NonNull String query,

    @NonNull String documentType,

    Integer documentTypeYear,

    List<String> conversationAttributes,

    List<String> guessedAttributes) {
}
