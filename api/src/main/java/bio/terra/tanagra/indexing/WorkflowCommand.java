package bio.terra.tanagra.indexing;

import java.util.Map;

public class WorkflowCommand {

  /** The different Apache Beam workflows supported by Tanagra. */
  public enum Type {
    ALL_ATTRIBUTES,
    TEXT_SEARCH, // for entity definintion
    PARENT_CHILD,
    ANCESTOR_DESCENDANT,
    NODE_PATH, // for entity hierarchy
    STATIC_COUNT; // for CriteriaOccurrence entity group
  }

  private Type type;
  private Map<String, String> descToInputStr;

  public WorkflowCommand(Type type, Map<String, String> descToInputStr) {
    this.type = type;
    this.descToInputStr = descToInputStr;
  }
}
