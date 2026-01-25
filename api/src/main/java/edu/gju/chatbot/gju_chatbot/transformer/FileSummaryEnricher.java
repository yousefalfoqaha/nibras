package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;

import edu.gju.chatbot.gju_chatbot.metadata.MetadataKeys;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileSummaryEnricher implements Function<Document, Document> {

  private Logger log = LoggerFactory.getLogger(FileSummaryEnricher.class);

  private static final PromptTemplate PROMPT_TEMPLATE = new PromptTemplate(
      """
                  Create a clear, descriptive headline for the provided text. It should serve as a label for the file. Only output plain text.

                  DOCUMENT:
                  <<<
                  {file_content}
                  >>>
          """);

  private final ChatModel chatModel;

  public Document enrich(Document document) {
    return apply(document);
  }

  @Override
  public Document apply(Document document) {
    String fileSummary = this.chatModel.call(
        PROMPT_TEMPLATE.create(Map.of("file_content", document.getText())))
        .getResult()
        .getOutput()
        .getText();

    log.debug("Summary generated: {}", fileSummary);

    document.getMetadata().put(MetadataKeys.FILE_SUMMARY, fileSummary);

    return document;
  }
}
