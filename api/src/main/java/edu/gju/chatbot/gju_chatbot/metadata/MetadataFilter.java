package edu.gju.chatbot.gju_chatbot.metadata;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MetadataFilter {
  private String name;
  private String description;
  private List<MetadataFilterOption> options = new ArrayList<>();

  @JsonIgnore
  public String getFormattedFilter() {
    StringBuilder sb = new StringBuilder();
    sb.append("Filter: ").append(this.name);

    if (this.description != null && !this.description.isEmpty()) {
      sb.append(" - ").append(this.description);
    }
    sb.append("\n");

    if (this.options != null && !this.options.isEmpty()) {
      sb.append("Options:\n");
      for (MetadataFilterOption option : this.options) {
        sb.append("  - ").append(option.getValue());

        if (option.getDescription() != null && !option.getDescription().isEmpty()) {
          sb.append(": ").append(option.getDescription());
        }

        sb.append("\n");
      }
    }

    return sb.toString().trim();
  }
}
