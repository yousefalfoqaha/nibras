package edu.gju.chatbot.gju_chatbot.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

public class StudyPlanAdvisor implements BaseAdvisor {
  private static final String SYSTEM_TEXT = """
      GJU Study Plan Structure Schema
      When interpreting documents, adhere to the following definitions:
      - Study Plan Section: A section is defined by two properties:
          1. Level: (University, School, Program/Department).
          2. Type: (Compulsory/Requirement, Elective, Remedial).
      - Requirements:
          - Course Requirements: Refer to the "Compulsory" or "Elective" courses within a section.
          - Admission Requirements: Refer to non-course conditions, like passing a Placement/Admission Test (e.g., Arabic, English, or Math).
      - Remedial Courses: Are courses required only based on the results of the Admission Tests.
      - Semesters: The semesters listed in the study plan are suggestions for course order, not rigid requirements.
      """;

  private int order = 1;

  @Override
  public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
    return chatClientRequest.mutate()
        .prompt(chatClientRequest.prompt().augmentSystemMessage(SYSTEM_TEXT))
        .build();
  }

  @Override
  public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
    return chatClientResponse;
  }

  @Override
  public int getOrder() {
    return this.order;
  }
}
