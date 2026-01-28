package edu.gju.chatbot.gju_chatbot.metadata;

import java.util.Set;

public final class MetadataKeys {

  public static final String FILE_ID = "file_id";

  public static final String FILE_NAME = "file_name";

  public static final String FILE_SIZE = "file_size";

  public static final String TITLE = "title";

  public static final String BREADCRUMBS = "breadcrumbs";

  public static final String SECTION_ID = "section_id";

  public static final String CHUNK_INDEX = "chunk_index";

  public static final String DOCUMENT_TYPE = "document_type";

  public static final String ACADEMIC_LEVEL = "academic_level";

  public static final String DEPARTMENT = "department";

  public static final String PROGRAM = "program";

  public static final String YEAR = "year";

  // prevent instantiation
  private MetadataKeys() {
  }

  public static final Set<String> ALL_KEYS = Set.of(
      FILE_ID, FILE_NAME, FILE_SIZE, TITLE, BREADCRUMBS,
      SECTION_ID, CHUNK_INDEX, DOCUMENT_TYPE, ACADEMIC_LEVEL, DEPARTMENT, PROGRAM, YEAR);
}
