package edu.gju.chatbot.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

public class DocumentSearchAdvisor implements BaseAdvisor {

    private static final String SYSTEM_MESSAGE = """
            Extract filters explicitly mentioned in the conversation, track missing required filters, decide if a clarifying question is needed.

            Do not imply you are using "filters", keep it conversational.
        """;

    private final int order = 2;

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
