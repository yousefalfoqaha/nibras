package edu.gju.chatbot.gju_chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class Controller {

  private final ChatClient chatClient;

  @GetMapping
  public String streamResponse(String userInput) {
    return chatClient.prompt(userInput)
        .call()
        .content();
  }
}
