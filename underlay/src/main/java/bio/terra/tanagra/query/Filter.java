package bio.terra.tanagra.query;

import java.util.List;

public abstract class Filter {
  /** Enum for the types of table filters supported by Tanagra. */
  public enum Type {
    BINARY,
    BOOLEAN_AND_OR
  }

  public abstract Type getType();

  public abstract FilterVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tables);
}
