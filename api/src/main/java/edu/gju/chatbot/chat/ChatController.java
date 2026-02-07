package edu.gju.chatbot.chat;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
public class ChatController {

  private final ChatClient chatClient;

  private final ChatService chatService;

  @GetMapping("/chat")
  public Flux<ServerSentEvent<Completion>> chat(
      @RequestParam(value = "message") String message,
      @RequestParam(value = "c", required = false) String conversationId) {

    String validConversationId = chatService.getValidConversationId(conversationId);

    return chatClient
        .prompt(message)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, validConversationId))
        .stream()
        .content()
        .map(token -> ServerSentEvent.builder(new Completion(token, validConversationId)).build());
  }

  @GetMapping("/chat/messages")
  public ResponseEntity<List<ChatMessage>> getChatMessages(@RequestParam(value = "c") String conversationId) {
    return new ResponseEntity<>(chatService.getConversation(conversationId), HttpStatus.OK);
  }
}
