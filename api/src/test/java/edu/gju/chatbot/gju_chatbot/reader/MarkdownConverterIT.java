package edu.gju.chatbot.gju_chatbot.reader;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.util.List;

@SpringBootTest
public class MarkdownConverterIT {

  private Resource resource = new DefaultResourceLoader().getResource("classpath:file_source.pdf");

  @Autowired
  MarkdownConverter markdownConverter;

  @Test
  void convertFileToMarkdown() {
    List<Document> pagesWithImages = markdownConverter.convert(resource);

    assertTrue(pagesWithImages.size() == 6);

    long textCount =  pagesWithImages.stream()
        .filter(Document::isText)
        .count();

    assertTrue(textCount == 3);
    assertTrue(pagesWithImages.get(0).isText() || pagesWithImages.get(1).isText());
    assertTrue(pagesWithImages.get(0).getMetadata().get("file_name").equals("file_source.pdf"), "File name is not the same.");
  }
}
