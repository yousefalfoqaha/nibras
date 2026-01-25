package edu.gju.chatbot.gju_chatbot.metadata;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/meta")
public class MetadataFilterController {

  private final MetadataFieldRepository metadataFieldRepository;

  @PostMapping("/files/process")
  public ResponseEntity<List<MetadataFilter>> getMetadata() {
    return new ResponseEntity<>(metadataFieldRepository.getMetadataFields(), HttpStatus.OK);
  }
}
