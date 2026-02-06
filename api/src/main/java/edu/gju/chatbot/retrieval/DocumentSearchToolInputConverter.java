package edu.gju.chatbot.retrieval;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.gju.chatbot.exception.RagException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentSearchToolInputConverter implements Converter<String, UserQuery> {

  private final ObjectMapper objectMapper;

  @Override
  public UserQuery convert(String toolInput) {
    DocumentSearchToolInput input;
    try {
      input = this.objectMapper.readValue(
          toolInput,
          DocumentSearchToolInput.class);
    } catch (IOException e) {
      throw new RagException(
          "Failed to parse tool input for document searching.",
          e);
    }

    Map<String, Object> confirmedAttributes = Optional.ofNullable(
        input.conversationAttributes())
        .map(a -> IntStream.range(0, a.size() / 2)
            .boxed()
            .collect(
                Collectors.toMap(
                    i -> a.get(i * 2),
                    i -> (Object) a.get(i * 2 + 1))))
        .orElse(Collections.emptyMap());

    return UserQuery.builder()
        .query(input.query())
        .documentType(input.documentType())
        .targetYear(input.documentTypeYear())
        .confirmedAttributes(confirmedAttributes)
        .build();
  }
}
