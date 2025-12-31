package edu.gju.chatbot.gju_chatbot.reader;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

@SpringBootTest
public class MarkdownConverterIT {

  private Resource resource = new DefaultResourceLoader().getResource("classpath:file_source.pdf");

  @Autowired
  MarkdownConverter markdownConverter;

  @Test
  void convertFileToMarkdown() {
    Document document = markdownConverter.convert(resource);

    assertTrue(document.getText().length() != 0, "Content is empty.");
    assertTrue(document.getMetadata().get("file_name").equals("file_source.pdf"), "File name is not the same.");
  }
}
