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
          You are given the full content of a single file.
          Your task is to produce a concise but information-dense global summary of the file.

          GOALS:
          - Capture the overall purpose, scope, and structure of the file
          - Identify the main topics, concepts, entities, and processes discussed
          - Preserve important relationships, constraints, assumptions, and definitions
          - Focus on information that would be useful for retrieval and downstream reasoning

          CONSTRAINTS:
          - Target length: approximately 500 tokens
          - Be factual and neutral in tone
          - Do NOT include examples unless they are essential to understanding the file
          - Do NOT speculate or add information not present in the text
          - Do NOT refer to this text as "the document" or "the file"
          - Do NOT include headings, bullet points, or markdown
          - Write in plain, continuous prose

          The summary should represent the global context of the entire file, not individual sections.

          FULL FILE CONTENT:
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
