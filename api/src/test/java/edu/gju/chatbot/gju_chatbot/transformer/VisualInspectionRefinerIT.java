package edu.gju.chatbot.gju_chatbot.transformer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import edu.gju.chatbot.gju_chatbot.reader.MarkdownConverter;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
public class VisualInspectionRefinerIT {

  private Resource resource = new DefaultResourceLoader().getResource("classpath:file_source.pdf");

  @Autowired
  VisualInspectionRefiner visualInspectionRefiner;

  @Autowired
  MarkdownConverter markdownConverter;

  @Test
  void testRefinePages() {
    List<Document> pagesWithImages = markdownConverter.convert(resource);
    List<Document> refinedPages = visualInspectionRefiner.apply(pagesWithImages);

    assertTrue(refinedPages.size() == (int) Math.floor(pagesWithImages.size() * 0.5));


    String fileContent = refinedPages.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n"));

    System.out.println(fileContent);
  }
}
