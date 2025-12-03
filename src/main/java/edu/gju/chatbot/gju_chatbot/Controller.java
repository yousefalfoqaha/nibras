package edu.gju.chatbot.gju_chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class Controller {
  private final ChatClient chatClient;

  @GetMapping("/generate")
  public String streamResponse(
      @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
    return chatClient.prompt(message)
        .call()
        .content();
  }
}
