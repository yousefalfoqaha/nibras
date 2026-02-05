package edu.gju.chatbot.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public final class ChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private final ChatMemory chatMemory;

    private final String defaultConversationId;

    private final int order;

    private final Scheduler scheduler;

    public ChatMemoryAdvisor(
        ChatMemory chatMemory,
        String defaultConversationId,
        int order,
        Scheduler scheduler
    ) {
        Assert.notNull(chatMemory, "chatMemory cannot be null");
        Assert.hasText(
            defaultConversationId,
            "defaultConversationId cannot be null or empty"
        );
        Assert.notNull(scheduler, "scheduler cannot be null");
        this.chatMemory = chatMemory;
        this.defaultConversationId = defaultConversationId;
        this.order = order;
        this.scheduler = scheduler;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public ChatClientRequest before(
        ChatClientRequest chatClientRequest,
        AdvisorChain advisorChain
    ) {
        String conversationId = chatClientRequest
            .context()
            .getOrDefault("conversationId", defaultConversationId)
            .toString();

        List<Message> memoryMessages = chatMemory
            .get(conversationId)
            .stream()
            .filter(m -> !(m instanceof SystemMessage))
            .collect(Collectors.toList());

        List<Message> processedMessages = new ArrayList<>(memoryMessages);
        processedMessages.addAll(chatClientRequest.prompt().getInstructions());

        ChatClientRequest processedRequest = chatClientRequest
            .mutate()
            .prompt(
                chatClientRequest
                    .prompt()
                    .mutate()
                    .messages(processedMessages)
                    .build()
            )
            .build();

        Message userMessage = processedRequest.prompt().getUserMessage();
        chatMemory.add(conversationId, userMessage);

        return processedRequest;
    }

    @Override
    public ChatClientResponse after(
        ChatClientResponse chatClientResponse,
        AdvisorChain advisorChain
    ) {
        String conversationId = chatClientResponse
            .context()
            .getOrDefault("conversationId", defaultConversationId)
            .toString();

        List<Message> assistantMessages = new ArrayList<>();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse
                .chatResponse()
                .getResults()
                .stream()
                .map(g -> (Message) g.getOutput())
                .filter(m -> !(m instanceof SystemMessage))
                .toList();
        }

        chatMemory.add(conversationId, assistantMessages);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest,
        StreamAdvisorChain streamAdvisorChain
    ) {
        return Mono.just(chatClientRequest)
            .publishOn(this.scheduler)
            .map(request -> this.before(request, streamAdvisorChain))
            .flatMapMany(streamAdvisorChain::nextStream)
            .transform(flux ->
                new ChatClientMessageAggregator().aggregateChatClientResponse(
                    flux,
                    response -> this.after(response, streamAdvisorChain)
                )
            );
    }

    public static Builder builder(ChatMemory chatMemory) {
        return new Builder(chatMemory);
    }

    public static final class Builder {

        private String conversationId = ChatMemory.DEFAULT_CONVERSATION_ID;

        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

        private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;

        private final ChatMemory chatMemory;

        private Builder(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public ChatMemoryAdvisor build() {
            return new ChatMemoryAdvisor(
                this.chatMemory,
                this.conversationId,
                this.order,
                this.scheduler
            );
        }
    }
}
