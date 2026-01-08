package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileSummaryEnricher implements DocumentTransformer {

  private Logger log = LoggerFactory.getLogger(FileSummaryEnricher.class);

  private static final PromptTemplate PROMPT_TEMPLATE = new PromptTemplate(
      """
              Generate a concise, neutral file name based on the document.

              DOCUMENT:
              <<<
              {file_content}
              >>>
          """);

  private final ChatModel chatModel;

  @Override
  public List<Document> apply(List<Document> documents) {
    log.info("Recieved {} documents", documents.size());

    String fileContent = documents.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n"));

    String fileSummary = this.chatModel.call(
        PROMPT_TEMPLATE.create(Map.of("file_content", fileContent)))
        .getResult()
        .getOutput()
        .getText();

    log.debug("Summary generated: {}", fileSummary);

    for (Document document : documents) {
      document.getMetadata().put(DocumentMetadataKeys.FILE_SUMMARY, fileSummary);
    }

    return documents;
  }
}
