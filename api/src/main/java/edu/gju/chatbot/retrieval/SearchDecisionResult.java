package edu.gju.chatbot.retrieval;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SearchDecisionResult {

  private UserQuery userQuery;

  private Optional<String> interruptMessage;

  public static SearchDecisionResult fromContext(SearchDecisionContext context) {
    return new SearchDecisionResult(
        context.getUserQuery(),
        Optional.ofNullable(context.getInterruptMessage()));
  }
}
