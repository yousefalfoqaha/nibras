package edu.gju.chatbot.chat;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ChatService {

  private final ChatMemoryRepository chatMemoryRepository;

  public String getValidConversationId(String conversationId) {
    List<String> conversationIds = chatMemoryRepository.findConversationIds();

    String validatedConversationId = conversationId;

    if (!conversationIds.contains(conversationId)) {
      validatedConversationId = UUID.randomUUID().toString();
    }

    return validatedConversationId;
  }

  public List<ChatMessage> getConversation(String conversationId) {
    return chatMemoryRepository.findByConversationId(conversationId).stream()
        .filter(
            m ->
                m.getMessageType() == MessageType.ASSISTANT
                    || m.getMessageType() == MessageType.USER)
        .map(m -> new ChatMessage(m.getText(), m.getMessageType().toString()))
        .toList();
  }
}
