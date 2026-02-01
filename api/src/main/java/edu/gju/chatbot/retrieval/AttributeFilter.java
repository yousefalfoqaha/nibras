package edu.gju.chatbot.retrieval;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class AttributeFilter {

    private Object value;

    private AttributeFilterReason reason;

    public enum AttributeFilterReason {
        CONVERSATION,
        GUESS,
    }
}
