package edu.gju.chatbot.gju_chatbot.common;

import java.util.Date;

public record ErrorObject(
    int statusCode,
    String message,
    Date timestamp) {
}
