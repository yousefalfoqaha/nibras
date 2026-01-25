package edu.gju.chatbot.gju_chatbot.etl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class EtlPipelineController {

  private final EtlPipelineService etlPipelineService;

  @PostMapping("/files/process")
  public ResponseEntity<Void> ingestFile(@RequestParam("file") MultipartFile file) {
    etlPipelineService.processFile(file.getResource());

    return ResponseEntity.ok().build();
  }
}
