package edu.gju.chatbot.etl;

import edu.gju.chatbot.exception.UnsupportedFileTypeException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EtlPipelineService {

    private final OcrScanner ocrScanner;

    private final MarkdownHierarchyEnricher markdownHierarchyEnricher;

    private final MarkdownTextSplitter markdownHeaderTextSplitter;

    private final FileMetadataEnricher fileMetadataEnricher;

    private final VectorStore vectorStore;

    public void processFile(Resource file) {
        String fileName = file.getFilename();
        String fileType = fileName.substring(fileName.lastIndexOf('.') + 1);

        if (!fileType.equals("pdf")) {
            throw new UnsupportedFileTypeException("Only PDFs are supported.");
        }

        Document ocrScan = ocrScanner.scan(file);

        Document enrichedMarkdownHierarchy = markdownHierarchyEnricher.enrich(
            ocrScan
        );

        Document enrichedSummary = fileMetadataEnricher.enrich(
            enrichedMarkdownHierarchy
        );

        List<Document> splitChunks = markdownHeaderTextSplitter.split(
            enrichedSummary
        );

        vectorStore.add(splitChunks);
    }
}
