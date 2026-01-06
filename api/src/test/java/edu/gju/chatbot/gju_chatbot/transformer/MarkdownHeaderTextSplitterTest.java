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

    assertEquals("Introduction", first.getMetadata().get("breadcrumbs"));
    assertEquals("Introduction > Background", second.getMetadata().get("breadcrumbs"));
    assertEquals("Introduction > Background > Details", third.getMetadata().get("breadcrumbs"));
    assertEquals("Introduction > Scope", fourth.getMetadata().get("breadcrumbs"));

    assertEquals("This is the intro text.", first.getText());
    assertEquals("Some background info.", second.getText());
    assertEquals("Fine-grained details.", third.getText());
    assertEquals("Scope description.", fourth.getText());

    assertEquals(3, fourth.getMetadata().get("chunk_index"));
  }
}
