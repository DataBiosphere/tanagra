package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;

/** A table within a {@link Underlay}. */
@AutoValue
public abstract class Table {
  /** The name of the table. */
  public abstract String name();

  /** The {@link BigQueryDataset} that contains this table. */
  // TODO consider how to support multiple types of databases.
  public abstract BigQueryDataset dataset();

  public static Table create(String name, BigQueryDataset dataset) {
    return new AutoValue_Table(name, dataset);
  }
}
