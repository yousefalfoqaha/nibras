package edu.gju.chatbot.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

public class RagAdvisor implements BaseAdvisor {

    private static final String SYSTEM_MESSAGE = """
            YOUR IDENTITY:
            You are an AI assistant named 'Nibras' for the German Jordanian University (GJU) that helps students find information.

            OUTPUT FORMAT:
            - Respond in Markdown.
            - Use headings, bullet lists, tables, and block quotes where necessary.
            - Preserve all relevant information from the documents.

            CONTEXT RULES:
            - When answering with document context, include the year if mentioned.

            Your answers must be direct, clear, and based on available information.
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
