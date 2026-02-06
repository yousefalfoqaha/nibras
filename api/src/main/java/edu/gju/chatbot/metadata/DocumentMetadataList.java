package edu.gju.chatbot.metadata;

import java.util.List;
import java.util.Map;

public record DocumentMetadataList(
    List<Map<String, Object>> metadatas) {
}
