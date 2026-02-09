package edu.gju.chatbot.retrieval;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class UserQuery {

  private String query;

  private String documentType;

  private Integer targetYear;

  private Map<String, Object> confirmedAttributes;

  public static Builder builder() {
    return new Builder();
  }

  public Builder mutate() {
    return new Builder(this);
  }

  @NoArgsConstructor
  public static class Builder {
    private String query;
    private String documentType;
    private Integer targetYear;
    private Map<String, Object> confirmedAttributes;

    public Builder(UserQuery userQuery) {
      this.query = userQuery.query;
      this.documentType = userQuery.documentType;
      this.targetYear = userQuery.targetYear;
      this.confirmedAttributes = userQuery.confirmedAttributes;
    }

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder documentType(String documentType) {
      this.documentType = documentType;
      return this;
    }

    public Builder targetYear(Integer targetYear) {
      this.targetYear = targetYear;
      return this;
    }

    public Builder confirmedAttributes(Map<String, Object> confirmedAttributes) {
      this.confirmedAttributes = confirmedAttributes;
      return this;
    }

    public UserQuery build() {
      return new UserQuery(this.query, this.documentType, this.targetYear, this.confirmedAttributes);
    }
  }
}
