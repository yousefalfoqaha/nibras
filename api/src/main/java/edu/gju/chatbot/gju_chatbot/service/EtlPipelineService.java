package edu.gju.chatbot.gju_chatbot.service;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.gju.chatbot.gju_chatbot.exception.FileProcessingException;
import edu.gju.chatbot.gju_chatbot.exception.UnsupportedFileTypeException;
import edu.gju.chatbot.gju_chatbot.reader.MarkdownConverter;
import edu.gju.chatbot.gju_chatbot.transformer.FileSummaryEnricher;
import edu.gju.chatbot.gju_chatbot.transformer.MarkdownHeaderTextSplitter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EtlPipelineService {

  private final MarkdownConverter markdownConverter;

  private final MarkdownHeaderTextSplitter markdownHeaderTextSplitter;

  private final FileSummaryEnricher fileSummaryEnricher;

  private final VectorStore vectorStore;

  public void processFile(MultipartFile file) {
    String fileName = file.getOriginalFilename();
    String fileType = fileName.substring(fileName.lastIndexOf('.') + 1);

    if (!fileType.equals("pdf")) {
      throw new UnsupportedFileTypeException("Only PDFs are supported.");
    }

    Resource resource;

    try {
      resource = new ByteArrayResource(file.getBytes());
    } catch (IOException e) {
      throw new FileProcessingException("Something went wrong with processing the file.");
    }

    Document ocrScan = markdownConverter.convert(resource);

    Document document = 

    List<Document> splitChunks = markdownHeaderTextSplitter.transform(List.of(document));

    List<Document> enrichedChunks = fileSummaryEnricher.transform(splitChunks);

    vectorStore.add(enrichedChunks);
  }
}
