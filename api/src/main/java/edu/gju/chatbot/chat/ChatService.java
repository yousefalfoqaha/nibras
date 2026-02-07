package edu.gju.chatbot.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ChatService {

  private final ChatMemoryRepository chatMemoryRepository;

  public String validateConversationId(String conversationId) {
    List<String> conversationIds = chatMemoryRepository.findConversationIds();

    String validatedConversationId = conversationId;

    if (!conversationIds.contains(conversationId)) {
      validatedConversationId = UUID.randomUUID().toString();
    }

    return validatedConversationId;
  }
}
