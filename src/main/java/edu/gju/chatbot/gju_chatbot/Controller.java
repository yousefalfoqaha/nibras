package edu.gju.chatbot.gju_chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.gju.chatbot.gju_chatbot.service.EtlPipelineService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class Controller {
  private final ChatClient chatClient;

  private final EtlPipelineService etlPipelineService;

  @GetMapping("/generate")
  public String generate(
      @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
    return chatClient.prompt(message)
        .call()
        .content();
  }

  @PostMapping("/ingest")
  public ResponseEntity<Void> ingestFile(
      @RequestParam("file") MultipartFile file) {
    etlPipelineService.processFile(file);

    return ResponseEntity.ok().build();
  }
}
