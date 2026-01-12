package edu.gju.chatbot.gju_chatbot.transformer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class MarkdownHeaderTextSplitterTest {

  MarkdownTextSplitter textSplitter;

  @BeforeEach
  void setUp() {
    textSplitter = new MarkdownTextSplitter(new TokenTextSplitter(
        128,
        64,
        10,
        5000,
        true));
  }

  @Test
  public void testExtractMarkdownHierarchy() {
    Document markdown = new Document("""
        # Introduction
        This is the intro text.

        ## Background
        Some background info.

        ### Details
        Fine-grained details.

        ## Scope
        Scope description.
        """,
        Map.of("source", "page1.pdf"));

    List<Document> chunks = textSplitter.split(markdown);

    assertEquals(4, chunks.size());

    var first = chunks.get(0);
    var second = chunks.get(1);
    var third = chunks.get(2);
    var fourth = chunks.get(3);
    //
    // // formatted breadcrumbs are stored under FORMATTED_BREADCRUMBS
    // assertEquals("Introduction", (String)
    // first.getMetadata().get(DocumentMetadataKeys.FORMATTED_BREADCRUMBS));
    // assertEquals("Introduction > Background",
    // (String)
    // second.getMetadata().get(DocumentMetadataKeys.FORMATTED_BREADCRUMBS));
    // assertEquals("Introduction > Background > Details",
    // (String)
    // third.getMetadata().get(DocumentMetadataKeys.FORMATTED_BREADCRUMBS));
    // assertEquals("Introduction > Scope", (String)
    // fourth.getMetadata().get(DocumentMetadataKeys.FORMATTED_BREADCRUMBS));
    //
    // // verify chunk texts
    // assertEquals("This is the intro text.", first.getText().trim());
    // assertEquals("Some background info.", second.getText().trim());
    // assertEquals("Fine-grained details.", third.getText().trim());
    // assertEquals("Scope description.", fourth.getText().trim());
    //
    // // chunk index stored under CHUNK_INDEX (ensure numeric comparison)
    // assertEquals(3, ((Number)
    // fourth.getMetadata().get(DocumentMetadataKeys.CHUNK_INDEX)).intValue());
    //
    // // --- New: check the breadcrumb objects themselves ---
    // // Note: Breadcrumb[] is an inner static class of MarkdownTextSplitter
    // var firstBreadcrumbs = (MarkdownTextSplitter.Breadcrumb[])
    // first.getMetadata()
    // .get(DocumentMetadataKeys.BREADCRUMBS);
    // var secondBreadcrumbs = (MarkdownTextSplitter.Breadcrumb[])
    // second.getMetadata()
    // .get(DocumentMetadataKeys.BREADCRUMBS);
    // var thirdBreadcrumbs = (MarkdownTextSplitter.Breadcrumb[])
    // third.getMetadata()
    // .get(DocumentMetadataKeys.BREADCRUMBS);
    // var fourthBreadcrumbs = (MarkdownTextSplitter.Breadcrumb[])
    // fourth.getMetadata()
    // .get(DocumentMetadataKeys.BREADCRUMBS);
    //
    // // first chunk: only H1
    // assertNotNull(firstBreadcrumbs);
    // assertNotNull(firstBreadcrumbs[0]);
    // assertEquals("Introduction", firstBreadcrumbs[0].getText());
    // assertEquals(1, firstBreadcrumbs[0].getLevel());
    // assertNull(firstBreadcrumbs[1]);
    // assertNull(firstBreadcrumbs[2]);
    //
    // // second chunk: H1 -> H2
    // assertNotNull(secondBreadcrumbs);
    // assertNotNull(secondBreadcrumbs[0]);
    // assertNotNull(secondBreadcrumbs[1]);
    // assertEquals("Introduction", secondBreadcrumbs[0].getText());
    // assertEquals(1, secondBreadcrumbs[0].getLevel());
    // assertEquals("Background", secondBreadcrumbs[1].getText());
    // assertEquals(2, secondBreadcrumbs[1].getLevel());
    // assertNull(secondBreadcrumbs[2]);
    //
    // // third chunk: H1 -> H2 -> H3
    // assertNotNull(thirdBreadcrumbs);
    // assertNotNull(thirdBreadcrumbs[0]);
    // assertNotNull(thirdBreadcrumbs[1]);
    // assertNotNull(thirdBreadcrumbs[2]);
    // assertEquals("Introduction", thirdBreadcrumbs[0].getText());
    // assertEquals("Background", thirdBreadcrumbs[1].getText());
    // assertEquals("Details", thirdBreadcrumbs[2].getText());
    // assertEquals(3, thirdBreadcrumbs[2].getLevel());
    //
    // // fourth chunk (Scope): H1 -> H2 (Scope). H3 should be cleared after Scope
    // // header.
    // assertNotNull(fourthBreadcrumbs);
    // assertNotNull(fourthBreadcrumbs[0]);
    // assertNotNull(fourthBreadcrumbs[1]);
    // assertEquals("Introduction", fourthBreadcrumbs[0].getText());
    // assertEquals("Scope", fourthBreadcrumbs[1].getText());
    // assertNull(fourthBreadcrumbs[2]);
  }
}
