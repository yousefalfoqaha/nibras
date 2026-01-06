package edu.gju.chatbot.gju_chatbot.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

public class MarkdownHeaderTextSplitterTest {

  MarkdownHeaderTextSplitter textSplitter;

  @BeforeEach
  void setUp() {
    textSplitter = new MarkdownHeaderTextSplitter();
  }

  @Test
  public void testExtractMarkdownHierarchy() {
    List<Document> pages = List.of(
        new Document("""
            # Introduction
            This is the intro text.

            ## Background
            Some background info.

            ### Details
            Fine-grained details.

            ## Scope
            Scope description.
            """,
            Map.of("source", "page1.pdf")));

    List<Document> chunks = textSplitter.transform(pages);

    assertEquals(4, chunks.size());

    var first = chunks.get(0);
    var second = chunks.get(1);
    var third = chunks.get(2);
    var fourth = chunks.get(3);

    assertEquals("Introduction", first.getMetadata().get("h1"));
    assertEquals("", first.getMetadata().get("h2"));
    assertEquals("", first.getMetadata().get("h3"));

    assertEquals("Introduction", second.getMetadata().get("h1"));
    assertEquals("Background", second.getMetadata().get("h2"));
    assertEquals("", second.getMetadata().get("h3"));

    assertEquals("Introduction", third.getMetadata().get("h1"));
    assertEquals("Background", third.getMetadata().get("h2"));
    assertEquals("Details", third.getMetadata().get("h3"));

    assertEquals("Introduction", fourth.getMetadata().get("h1"));
    assertEquals("Scope", fourth.getMetadata().get("h2"));
    assertEquals("", fourth.getMetadata().get("h3"));

    assertEquals(3, fourth.getMetadata().get("chunk_index"));
  }
}
