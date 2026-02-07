package edu.gju.chatbot.chat;

public record ChatRequest(
    String conversationId,
    String message) {
}
