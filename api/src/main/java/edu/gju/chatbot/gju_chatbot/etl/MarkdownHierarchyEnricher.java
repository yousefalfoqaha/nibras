package edu.gju.chatbot.gju_chatbot.etl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.core.ParameterizedTypeReference;

public class MarkdownHierarchyEnricher implements Function<Document, Document> {

    private static final Logger log = LoggerFactory.getLogger(
        MarkdownHierarchyEnricher.class
    );

    private static final String SYSTEM_PROMPT = """
        You are refining an OCR-scanned document within a RAG ETL pipeline that has been converted to markdown.
        The headers detected are only rough approximations and don't dictate the true hierarchy of the text.
        The pipeline has already generated a global summary for this file. You do not need to capture the overall document title or subject in your header hierarchy.

        You are assigning hierarchy levels to headers (1 to 6) to generate **Breadcrumbs** for the text paragraphs between them.

        The ETL pipeline will use your output to extract the non-header parapgraph texts as chunks, and place the nearest header hierarchy to it (h1, h2, etc.) as breadcrumbs.

        The document is made up of several different sections, header level 1 is used to clearly segregate sections so the final chunks appear to come from different parent section within the same document.

        Header levels 2 to 6 are used as continuations after anchoring on header level 1 (complete topic change), don't nest headers if they are more appropriate as siblings, only nest when its clear they are parent-child.

        General example (all headers detected have been set to h2 for simplifying, but you may encounter several header levels the OCR detected. Again, the OCR is most likely wrong:

        <header-id-1>## German Jordanian University
        <header-id-2>## School of Computing
        <header-id-3>## Department of Computer Science
        <header-id-4>## Bachelor of Science in Computer Science
        <header-id-5>## Study Plan 2023-2024
        <header-id-6>## I. Program Objectives
        <header-id-7>## II. Learning Outcomes
        <header-id-8>## Course Delivery Methods
        <header-id-9>## III. Admission Requirements
        <header-id-10>## Placements Tests
        <header-id-11>## IV. Degree Requirements
        <header-id-12>## V. Framework for B.Sc. Degree (145 credit hours)
        <header-id-13>## 1. University Requirements: (27 credit hours)
        <header-id-14>## 1.1. Compulsory: (21 credit hours)
        <header-id-15>## 1.2. Elective: (6 credit hours) (Two courses out of the following)
        <header-id-16>## 2. School Requirements: (27 credit hours)
        <header-id-17>## 3. Program Requirements (91 credit hours)
        <header-id-18>## 3.1 Program Requirements (Compulsory): (79 credit hours)
        <header-id-19>## 3.1.A Common Compulsory Courses for all Tracks (67 credit hours):
        <header-id-20>## 3.1.B. Special Compulsory Courses for the General Track (12 credit hours):
        <header-id-21>## 3.1.C. Special Compulsory Courses for the Data Science Track (12 credit hours):
        <header-id-22>## 3.1.D. Special Compulsory Courses for the Cybersecurity Track (12 credit hours):
        <header-id-23>## 3.2. Program Requirements (Electives b): (12 credit hours)
        <header-id-24>## 3.2.A. List of Elective Courses for the General Track:
        <header-id-25>## 3.2.B. List of Elective Courses for the Data Science Track
        <header-id-26>## 3.2.C. List of Elective Courses for the Cybersecurity Track

        Output:

        {
          "<header-id-1>": 1,
          "<header-id-2>": -1,
          "<header-id-3>": -1,
          "<header-id-4>": -1,
          "<header-id-5>": -1,
          "<header-id-6>": 1,
          "<header-id-7>": 1,
          "<header-id-8>": 2,
          "<header-id-9>": 1,
          "<header-id-10>": 2,
          "<header-id-11>": 1,
          "<header-id-12>": 1,
          "<header-id-13>": 2,
          "<header-id-14>": 3,
          "<header-id-15>": 3,
          "<header-id-16>": 2,
          "<header-id-17>": 2,
          "<header-id-18>": 3,
          "<header-id-19>": 4,
          "<header-id-20>": 4,
          "<header-id-21>": 4,
          "<header-id-22>": 4,
          "<header-id-23>": 3,
          "<header-id-24>": 4,
          "<header-id-25>": 4,
          "<header-id-26>": 4,
        }

        Output only JSON, the key is the header ID (including the <> brackets), value is the header level.
            """;

    private final ChatClient chatClient;

    public MarkdownHierarchyEnricher(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Document enrich(Document markdown) {
        return apply(markdown);
    }

    @Override
    public Document apply(Document document) {
        List<String> linesWithHeaderIds = addHeaderIds(document.getText());

        Map<String, Integer> correctedHeaders = chatClient
            .prompt()
            .user(u -> u.text(String.join("\n", linesWithHeaderIds)))
            .system(s -> s.text(SYSTEM_PROMPT))
            .call()
            .entity(new ParameterizedTypeReference<Map<String, Integer>>() {});

        System.out.println(correctedHeaders.toString());

        List<String> enrichedLines = new ArrayList<>();
        for (String line : linesWithHeaderIds) {
            if (!line.startsWith("<header-id-")) {
                enrichedLines.add(line);
                continue;
            }

            int startHeaderIndex = line.indexOf(">") + 1;
            String headerId = line.substring(0, startHeaderIndex);
            Integer level = correctedHeaders.get(headerId);

            String text = line.substring(startHeaderIndex).stripLeading();

            if (level == null || level == -1) {
                text = text.replaceFirst("^#+\\s*", "");
                enrichedLines.add(text);
            } else {
                text = text.replaceFirst("^#+\\s*", "");
                enrichedLines.add("#".repeat(level) + " " + text);
            }
        }

        log.info(String.join("\n", enrichedLines));

        return new Document(
            String.join("\n", enrichedLines),
            document.getMetadata()
        );
    }

    private static List<String> addHeaderIds(String text) {
        AtomicInteger counter = new AtomicInteger(1);
        String[] lines = text.split("\n", -1);

        List<String> result = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("#")) {
                result.add(
                    "<header-id-" + counter.getAndIncrement() + ">" + line
                );
            } else {
                result.add(line);
            }
        }

        return result;
    }
}
