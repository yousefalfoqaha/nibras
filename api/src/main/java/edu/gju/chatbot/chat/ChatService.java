package edu.gju.chatbot.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

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
    List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);

    for (Message m : messages) {
      System.out.println(m.getText());
      System.out.println(m.getMessageType());
    }

    return messages
        .stream()
        .filter(m -> m.getMessageType() == MessageType.ASSISTANT || m.getMessageType() == MessageType.USER)
        .map(m -> new ChatMessage(m.getText(), m.getMessageType().toString()))
        .toList();
  }
}
