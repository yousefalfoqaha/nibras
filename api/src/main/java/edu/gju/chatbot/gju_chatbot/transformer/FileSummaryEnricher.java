package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class FileSummaryEnricher {

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

  private static final String FILE_SUMMARY_METADATA_KEY = "file_summary";

  private final ChatModel chatModel;

  public List<Document> enrich(List<Document> documents) {
    String fileContent = documents.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n"));

    String fileSummary = this.chatModel.call(
        PROMPT_TEMPLATE.create(Map.of("file_content", fileContent)))
        .getResult()
        .getOutput()
        .getText();

    for (Document document : documents) {
      document.getMetadata().put(FILE_SUMMARY_METADATA_KEY, fileSummary);
    }

    return documents;

  }
}
