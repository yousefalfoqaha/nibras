package edu.gju.chatbot.gju_chatbot.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

@Service
public class TokenTextSplitterService {

  public List<Document> splitDocuments(List<Document> documents) {
    TokenTextSplitter splitter = new TokenTextSplitter();
    return splitter.apply(documents);
  }

  public List<Document> splitCustomized(List<Document> documents) {
    TokenTextSplitter splitter = new TokenTextSplitter(1000, 400, 10, 5000, true);
    return splitter.apply(documents);
  }
}
