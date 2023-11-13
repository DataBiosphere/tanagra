package bio.terra.tanagra.query;

public abstract class DataPointer {

  /** Enum for the types of external data pointers supported by Tanagra. */
  public enum Type {
    BQ_DATASET
  }

  private final String name;

  public DataPointer(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract String getTableSQL(String tableName);
}
