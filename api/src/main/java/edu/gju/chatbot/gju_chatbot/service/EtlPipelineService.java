package edu.gju.chatbot.gju_chatbot.service;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.gju.chatbot.gju_chatbot.reader.PdfDocumentReader;
import edu.gju.chatbot.gju_chatbot.transformer.ChunkContextEnricher;
import edu.gju.chatbot.gju_chatbot.exception.FileProcessingException;
import edu.gju.chatbot.gju_chatbot.exception.UnsupportedFileTypeException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EtlPipelineService {
  private final VectorStore vectorStore;
  private final PdfDocumentReader pdfDocumentReader;
  private final ChunkContextEnricher chunkContextEnricher;

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

    List<Document> documents = pdfDocumentReader.apply(resource);

    for (Document document : documents) {
      System.out.println("\n----------------\n\n");
      System.out.println(document.getText());
    }

    // List<Document> chunks = new TokenTextSplitter().split(documents);

    List<Document> enrichedChunks = chunkContextEnricher.apply(chunks);

    vectorStore.add(enrichedChunks);
  }
}
