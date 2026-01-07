package edu.gju.chatbot.gju_chatbot.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import edu.gju.chatbot.gju_chatbot.exception.UnsupportedFileTypeException;
import edu.gju.chatbot.gju_chatbot.reader.MarkdownConverter;
import edu.gju.chatbot.gju_chatbot.transformer.FileSummaryEnricher;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownHeaderTextSplitter;
import edu.gju.chatbot.gju_chatbot.transformer.VisualInspectionRefiner;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EtlPipelineService {

  private final MarkdownConverter markdownConverter;

  private final VisualInspectionRefiner visualInspectionRefiner;

  private final MarkdownHeaderTextSplitter markdownHeaderTextSplitter;

  private final FileSummaryEnricher fileSummaryEnricher;

  private final VectorStore vectorStore;

  public void processFile(Resource file) {
    String fileName = file.getFilename();
    String fileType = fileName.substring(fileName.lastIndexOf('.') + 1);

    if (!fileType.equals("pdf")) {
      throw new UnsupportedFileTypeException("Only PDFs are supported.");
    }

    List<Document> pagesWithImages = markdownConverter.convert(file);

    List<Document> refinedPages = visualInspectionRefiner.apply(pagesWithImages);

    List<Document> enrichedPages = fileSummaryEnricher.transform(refinedPages);

    List<Document> splitChunks = markdownHeaderTextSplitter.transform(enrichedPages);

    vectorStore.add(splitChunks);
  }
}
