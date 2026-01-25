package edu.gju.chatbot.gju_chatbot.metadata;

import java.util.ArrayList;
import java.util.List;

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

  private List<MetadataFilterValue> allowed = new ArrayList<>();
}
