package edu.gju.chatbot.gju_chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.gju.chatbot.gju_chatbot.dto.TokenDto;
import edu.gju.chatbot.gju_chatbot.service.EtlPipelineService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
@SpringBootApplication
public class GjuChatbotApplication {

  private final ChatClient chatClient;

  private final EtlPipelineService etlPipelineService;

  @GetMapping("/chat")
  public Flux<ServerSentEvent<TokenDto>> generate(
      @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
    return chatClient.prompt(message)
        .stream()
        .content()
        .map(token -> ServerSentEvent.builder(new TokenDto(token)).build());
  }

  @PostMapping("/files/process")
  public ResponseEntity<Void> ingestFile(@RequestParam("file") MultipartFile file) {
    etlPipelineService.processFile(file);

    return ResponseEntity.ok().build();
  }

  public static void main(String[] args) {
    SpringApplication.run(GjuChatbotApplication.class, args);
  }

}
