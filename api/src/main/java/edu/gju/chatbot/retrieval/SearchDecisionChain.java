package edu.gju.chatbot.retrieval;

import java.util.Comparator;
import java.util.List;

public class SearchDecisionChain {

  private List<SearchDecisionHandler> handlers;

  public SearchDecisionChain(List<SearchDecisionHandler> handlers) {
    this.handlers = handlers.stream()
        .sorted(Comparator.comparingInt(SearchDecisionHandler::getOrder))
        .toList();
  }

  public SearchDecisionResult execute(UserQuery userQuery) {
    SearchDecisionContext context = SearchDecisionContext.builder(userQuery).build();

    for (SearchDecisionHandler handler : handlers) {
      context = handler.handle(context);

      if (context.isInterrupted())
        break;
    }

    return SearchDecisionResult.fromContext(context);
  }
}
