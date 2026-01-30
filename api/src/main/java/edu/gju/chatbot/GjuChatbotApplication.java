package edu.gju.chatbot;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
@SpringBootApplication
public class GjuChatbotApplication {

    private final ChatClient chatClient;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<TokenDto>> generate(
        @RequestParam(
            value = "message",
            defaultValue = "Who are you?"
        ) String message
    ) {
        return chatClient
            .prompt(message)
            .stream()
            .content()
            .map(token -> ServerSentEvent.builder(new TokenDto(token)).build());
    }

    public static void main(String[] args) {
        SpringApplication.run(GjuChatbotApplication.class, args);
    }

    private record TokenDto(String text) {}
}
