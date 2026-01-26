package edu.gju.chatbot.gju_chatbot.etl;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import edu.gju.chatbot.gju_chatbot.exception.UnsupportedFileTypeException;
import edu.gju.chatbot.gju_chatbot.reader.OcrScanner;
import edu.gju.chatbot.gju_chatbot.transformer.FileMetadataEnricher;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownTextSplitter;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownHierarchyEnricher;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EtlPipelineService {

  private final OcrScanner ocrScanner;

  private final MarkdownHierarchyEnricher markdownHierarchyEnricher;

  private final MarkdownTextSplitter markdownHeaderTextSplitter;

  private final FileMetadataEnricher fileSummaryEnricher;

  private final VectorStore vectorStore;

  public void processFile(Resource file) {
    String fileName = file.getFilename();
    String fileType = fileName.substring(fileName.lastIndexOf('.') + 1);

    if (!fileType.equals("pdf")) {
      throw new UnsupportedFileTypeException("Only PDFs are supported.");
    }

    Document ocrScan = ocrScanner.scan(file);

    Document enrichedMarkdownHierarchy = markdownHierarchyEnricher.enrich(ocrScan);

    Document enrichedSummary = fileSummaryEnricher.enrich(enrichedMarkdownHierarchy);

    List<Document> splitChunks = markdownHeaderTextSplitter.split(enrichedSummary);

    vectorStore.add(splitChunks);
  }
}
