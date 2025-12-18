package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ChunkContextEnricher implements DocumentTransformer {
  private final ChatModel chatModel;

  private static final String PROMPT_TEMPLATE = """
          <document>
          {whole_document}
          </document>

          <chunk>
          {chunk_content}
          </chunk>

          Please give a short succinct context to situate this chunk within the overall document for the purposes of improving search retrieval of the chunk.

          Someone reading only this chunk should understand exactly what document this belongs to, which section it's from, what comes before and after, and how it relates to the document's main themes.

          Then generate 3-5 FAQ-style questions that someone might ask when searching for this information.

          Format your response as:
          Context: [your contextual summary]
          Questions:
          - [question 1]
          - [question 2]
          - [question 3]

          Answer only with the succinct context and questions, nothing else.
      """;

  private static final PromptTemplate promptTemplate = PromptTemplate.builder()
      .template(PROMPT_TEMPLATE)
      .build();

  public List<Document> apply(List<Document> documents) {
    String wholeDocument = documents.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n"));

    return documents.stream()
        .map(d -> {
          String prompt = promptTemplate
              .render(Map.of(
                  "whole_document", wholeDocument,
                  "chunk_content", d.getText()));

          String enrichedContext = chatModel.call(prompt);

          String newText = enrichedContext + "\n\n" + d.getText();

          System.out.println(newText);
          return new Document(d.getId(), newText, d.getMetadata());
        })
        .collect(Collectors.toList());
  }
}
