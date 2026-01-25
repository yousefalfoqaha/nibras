package edu.gju.chatbot.gju_chatbot.metadata;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/meta")
public class MetadataFilterController {

  private final MetadataFilterRepository metadataFilterRepository;

  @GetMapping
  public ResponseEntity<List<MetadataFilter>> getMetadata() {
    return new ResponseEntity<>(metadataFilterRepository.fetchMetadataFilters(), HttpStatus.OK);
  }
}
