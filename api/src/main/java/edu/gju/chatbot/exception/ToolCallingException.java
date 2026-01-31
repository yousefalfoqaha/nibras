package edu.gju.chatbot.exception;

public class ToolCallingException extends RuntimeException {

    public ToolCallingException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
