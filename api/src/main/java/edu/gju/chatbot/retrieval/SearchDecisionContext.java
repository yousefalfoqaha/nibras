package edu.gju.chatbot.retrieval;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SearchDecisionContext {

  private UserQuery userQuery;

  private String interruptMessage;

  private Map<String, Object> metadata = new HashMap<>();

  private SearchDecisionContext(UserQuery userQuery, String interruptMessage, Map<String, Object> metadata) {
    this.userQuery = userQuery;
    this.interruptMessage = interruptMessage;
    this.metadata = metadata;
  }

  public static Builder builder(UserQuery userQuery) {
    return new Builder(userQuery);
  }

  public static Builder builder(SearchDecisionContext context) {
    return new Builder(context);
  }

  public Builder mutate() {
    return new Builder(this);
  }

  public UserQuery getUserQuery() {
    return userQuery;
  }

  public SearchDecisionContext withUserQuery(UserQuery userQuery) {
    return this.mutate().userQuery(userQuery).build();
  }

  public String getInterruptMessage() {
    return interruptMessage;
  }

  public Map<String, Object> getMetadata() {
    return Collections.unmodifiableMap(this.metadata);
  }

  public SearchDecisionContext withMetadata(String key, Object value) {
    Map<String, Object> newMetadata = new HashMap<>(this.metadata);
    newMetadata.put(key, value);

    return this.mutate().metadata(newMetadata).build();
  }

  public boolean isInterrupted() {
    return interruptMessage != null && !interruptMessage.isEmpty();
  }

  public SearchDecisionContext interrupted(String message) {
    return this.mutate().interruptMessage(message).build();
  }

  public static class Builder {

    private UserQuery userQuery;

    private String interruptMessage;

    private Map<String, Object> metadata = new HashMap<>();

    public Builder(UserQuery userQuery) {
      this.userQuery = userQuery;
    }

    public Builder(SearchDecisionContext context) {
      this.userQuery = context.userQuery;
      this.interruptMessage = context.interruptMessage;
      this.metadata = new HashMap<>(context.metadata);
    }

    public Builder userQuery(UserQuery userQuery) {
      this.userQuery = userQuery;
      return this;
    }

    public Builder interruptMessage(String interruptMessage) {
      this.interruptMessage = interruptMessage;
      return this;
    }

    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public SearchDecisionContext build() {
      return new SearchDecisionContext(userQuery, interruptMessage, metadata);
    }
  }
}
