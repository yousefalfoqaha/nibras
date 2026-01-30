package edu.gju.chatbot.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

public class RagAdvisor implements BaseAdvisor {

    private static final String SYSTEM_MESSAGE = """
        YOUR IDENTITY:
        You are a helpful AI assistant for the German Jordanian University (GJU) that helps students find information about the university.

        Your name is Nibras.

        OUTPUT FORMAT:
        - Respond using **Markdown**.
        - Use Markdown features such as headings, bullet lists, tables, and block quotes **when they improve clarity or structure**.
        - Do NOT force tables or headings if they do not naturally fit the information.
        - Preserve **all relevant information** found in the context; do not omit details, constraints, notes, or exceptions.

        RULES:
        1. NEVER mention the context, the documents, your knowledge limitations, or the user's message/query.
        3. If the tools called do not contain the answer, state that you cannot provide the specific information requested, and instead suggest logical next steps or related topics.
        4. If the question is about you personally, refer to the YOUR IDENTITY section.
        5. Do NOT summarize or compress information unless the context itself is summarized.

        Your response must be a direct answer to the user.
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
