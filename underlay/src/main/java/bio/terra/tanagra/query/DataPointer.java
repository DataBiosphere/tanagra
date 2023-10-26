package bio.terra.tanagra.query;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.bigquery.BigQueryDataset;
import bio.terra.tanagra.serialization.UFDataPointer;
import bio.terra.tanagra.serialization.datapointer.UFBigQueryDataset;

public abstract class DataPointer {

  /** Enum for the types of external data pointers supported by Tanagra. */
  public enum Type {
    BQ_DATASET
  }

  private final String name;

  public DataPointer(String name) {
    this.name = name;
  }

  public abstract Type getType();

  public abstract QueryExecutor getQueryExecutor();

  public String getName() {
    return name;
  }

  public abstract String getTableSQL(String tableName);

  public abstract String getTablePathForIndexing(String tableName);

  public UFDataPointer serialize() {
    if (getType().equals(Type.BQ_DATASET)) {
      return new UFBigQueryDataset((BigQueryDataset) this);
    } else {
      throw new InvalidConfigException("Unknown data pointer type: " + getType());
    }
  }

  public abstract Literal.DataType lookupDatatype(FieldPointer fieldPointer);
}
