package edu.gju.chatbot.gju_chatbot.reader;

import java.util.UUID;
import java.util.function.Function;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import edu.gju.chatbot.gju_chatbot.utils.DocumentMetadataKeys;

public class MarkdownConverter implements Function<Resource, List<Document>> {

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;

    public MarkdownConverter(RestClient restClient, RetryTemplate retryTemplate) {
        this.restClient = restClient;
        this.retryTemplate = retryTemplate;
    }

    public List<Document> convert(Resource file) {
        return this.apply(file);
    }

    @Override
    public List<Document> apply(Resource file) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file);

        byte[] zipBytes = retryTemplate.execute(context ->
                restClient.post()
                        .body(parts)
                        .retrieve()
                        .body(byte[].class)
        );

        List<Document> documents = new ArrayList<>();
        UUID fileId = UUID.randomUUID();
        String fileName = file.getFilename();

        try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                byte[] bytes = zipStream.readAllBytes();
                zipStream.closeEntry();

                String entryName = entry.getName();
                int dotIndex = entryName.lastIndexOf('.');
                if (dotIndex == -1) continue;

                int page = Integer.parseInt(entryName.substring(0, dotIndex));
                String type = entryName.substring(dotIndex + 1);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put(DocumentMetadataKeys.FILE_ID, fileId);
                metadata.put(DocumentMetadataKeys.FILE_NAME, fileName);
                metadata.put(DocumentMetadataKeys.PAGE, page);

                Document doc;
                if (type.equals("md")) {
                    doc = Document.builder()
                            .text(new String(bytes, StandardCharsets.UTF_8))
                            .metadata(metadata)
                            .build();
                } else if (type.equals("png")) {
                    doc = Document.builder()
                            .media(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(bytes)))
                            .metadata(metadata)
                            .build();
                } else {
                    continue;
                }

                documents.add(doc);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read ZIP file", e);
        }

        documents.sort(Comparator.comparingInt(d -> (Integer) d.getMetadata().get(DocumentMetadataKeys.PAGE)));

        return documents;
    }
}
