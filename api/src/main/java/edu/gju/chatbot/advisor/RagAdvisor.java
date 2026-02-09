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
                - Respond in Markdown using extensive formatting: headings, subheadings, bullet points, numbered lists, tables, bold/italic text, and block quotes.
                - Organize information visually with proper structure, make it scannable and easy to read.
                - Preserve all relevant information from the documents, do not summarize.

                CONTEXT RULES:
                - Always fetch documents instead of relying on the conversation history for facts.
                - When answering with document context, include the year if mentioned.
                - Only respond based on the context, if the context does not contain the answer, say you do not know.
                - Do not mention phrases like "the context" or "the documents" in your answer.

                Your answers must be a direct, clear, answer to the user.
            """;

    private final int order = 1;

    @Override
    public ChatClientRequest before(
            ChatClientRequest chatClientRequest,
            AdvisorChain advisorChain) {
        return chatClientRequest
                .mutate()
                .prompt(
                        chatClientRequest.prompt().augmentSystemMessage(SYSTEM_MESSAGE))
                .build();
    }

    @Override
    public ChatClientResponse after(
            ChatClientResponse chatClientResponse,
            AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
