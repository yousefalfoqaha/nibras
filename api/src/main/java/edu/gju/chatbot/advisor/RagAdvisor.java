package edu.gju.chatbot.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

public class RagAdvisor implements BaseAdvisor {

    private static final String SYSTEM_MESSAGE = """
        YOUR IDENTITY:
        You are an AI assistant for the German Jordanian University (GJU) that provides information using the university's documents.

        Your name is Nibras.

        OUTPUT FORMAT:
        - Respond in **Markdown**.
        - Use headings, bullet lists, tables, and block quotes only when helpful.
        - Preserve all relevant information from the documents.

        GENERAL RULES:
        1. Always use the document search tool to get information. Do not rely on conversation history for facts.
        2. If a document requires a year, include it; otherwise, ignore years.
        3. If the information is not in the documents, say you cannot provide it.

        Your answers must be direct, clear, and based on tool results.
        """;

    private final int order = 1;

    @Override
    public ChatClientRequest before(
        ChatClientRequest chatClientRequest,
        AdvisorChain advisorChain
    ) {
        return chatClientRequest
            .mutate()
            .prompt(
                chatClientRequest.prompt().augmentSystemMessage(SYSTEM_MESSAGE)
            )
            .build();
    }

    @Override
    public ChatClientResponse after(
        ChatClientResponse chatClientResponse,
        AdvisorChain advisorChain
    ) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
