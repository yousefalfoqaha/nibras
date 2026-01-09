package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.core.ParameterizedTypeReference;

public class MarkdownHierarchyEnricher implements Function<Document, Document> {

  private static final Logger log = LoggerFactory.getLogger(MarkdownHierarchyEnricher.class);

  private static final String SYSTEM_PROMPT = """
          You are refining a markdown document that was generated from OCR. The OCR-detected headers are only rough approximations, and your task is to determine the true parent-child hierarchy.

          Each line detected as a header by OCR has a unique ID in the format <header-id-X>.

          For each header line, assign a header level from 1 to 6 (1 = top-level section, 6 = sub-subsection). Use -1 if the line should not be treated as a header.

          Assign levels so that the resulting hierarchy is consistent and well-formed. The goal is for downstream processes to be able to associate any non-header text with the correct surrounding headers, preserving the structure of the document as parent → child headers. Avoid assigning multiple headers at the same level in a way that would obscure proper parent-child relationships.

          Output only JSON mapping header IDs to levels. Example:

          {
            "<header-id-1>": 1,
            "<header-id-2>": 3,
            "<header-id-3>": -1
          }
      """;

  private final ChatClient chatClient;

  public MarkdownHierarchyEnricher(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  public Document enrich(Document markdown) {
    return apply(markdown);
  }

  @Override
  public Document apply(Document document) {
    List<String> linesWithHeaderIds = addHeaderIds(document.getText());

    Map<String, Integer> correctedHeaders = chatClient.prompt()
        .user(u -> u.text(String.join("\n", linesWithHeaderIds)))
        .system(s -> s.text(SYSTEM_PROMPT))
        .call()
        .entity(new ParameterizedTypeReference<Map<String, Integer>>() {
        });

    List<String> enrichedLines = new java.util.ArrayList<>();
    for (String line : linesWithHeaderIds) {
      if (!line.startsWith("<header-id-")) {
        enrichedLines.add(line);
        continue;
      }

      int startHeaderIndex = line.indexOf(">") + 1;
      String headerId = line.substring(0, startHeaderIndex);
      Integer level = correctedHeaders.get(headerId);

      String text = line.substring(startHeaderIndex).stripLeading();

      if (level == null || level == -1) {
        text = text.replaceFirst("^#+\\s*", "");
        enrichedLines.add(text);
      } else {
        text = text.replaceFirst("^#+\\s*", "");
        enrichedLines.add("#".repeat(level) + " " + text);
      }
    }

    log.info(String.join("\n", enrichedLines));

    return new Document(String.join("\n", enrichedLines), document.getMetadata());
  }

  private static List<String> addHeaderIds(String text) {
    AtomicInteger counter = new AtomicInteger(1);
    String[] lines = text.split("\n", -1);

    List<String> result = new ArrayList<>();

    for (String line : lines) {
      if (line.startsWith("#")) {
        result.add("<header-id-" + counter.getAndIncrement() + ">" + line);
      } else {
        result.add(line);
      }
    }

    return result;
  }
}
