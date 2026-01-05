package edu.gju.chatbot.gju_chatbot.transformer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.ArrayList;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

public class VisualInspectionRefiner implements Function<List<Document>, List<Document>> {

  private static final String SYSTEM_PROMPT = """
      You are an OCR correction tool. Your task is to fix OCR-generated markdown 
      by comparing it with the original PDF page image.

      Fix any OCR errors, formatting issues, or missing content.
      Output ONLY the corrected markdown - no explanations, no preamble, no code fences.
      """;

  private final ChatModel chatModel;

  public VisualInspectionRefiner(ChatModel chatModel) {
    this.chatModel = chatModel;
  }

  @Override
  public List<Document> apply(List<Document> pagesWithImages) {
    List<Document> markdownPages = pagesWithImages.stream()
        .filter(Document::isText)
        .toList();

    List<Document> imagePages = pagesWithImages.stream()
        .filter(doc -> !doc.isText())
        .toList();

    List<Document> refinedPages = new ArrayList<>();

    for (int i = 0; i < Math.min(markdownPages.size(), imagePages.size()); i++) {
      Document markdownDocument = markdownPages.get(i);
      Document imageDocument = imagePages.get(i);

      Prompt prompt = new Prompt(
          List.of(
              new SystemMessage(SYSTEM_PROMPT),
              UserMessage.builder()
                .text(markdownDocument.getText())
                .media(List.of(imageDocument.getMedia()))
                .build()
          )
      );

      String correctedMarkdown = chatModel.call(prompt)
          .getResult()
          .getOutput()
          .getText();

      Document refinedDocument = new Document(
          correctedMarkdown,
          markdownDocument.getMetadata()
      );

      refinedPages.add(refinedDocument);
    }

    return refinedPages;
  }
}
