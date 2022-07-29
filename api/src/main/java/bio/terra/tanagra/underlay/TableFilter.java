package bio.terra.tanagra.underlay;

public abstract class TableFilter {
  /** Enum for the types of table filters supported by Tanagra. */
  public enum Type {
    BINARY,
    ARRAY;
  }

  public enum BinaryOperator {
    EQUALS,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN_OR_EQUAL;
  }

  public enum LogicalOperator {
    AND,
    OR;
  }

  public TableFilter() {}
}
