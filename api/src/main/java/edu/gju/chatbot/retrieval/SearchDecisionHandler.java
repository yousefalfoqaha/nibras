package edu.gju.chatbot.retrieval;

import org.springframework.core.Ordered;

public interface SearchDecisionHandler extends Ordered {

  public SearchDecisionContext handle(SearchDecisionContext context);
}
