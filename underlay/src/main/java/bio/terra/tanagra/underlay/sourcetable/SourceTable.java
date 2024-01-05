package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.ColumnSchema;
import com.google.common.collect.ImmutableList;

public abstract class SourceTable {
  protected BQTable bqTable;

  protected SourceTable(BQTable bqTable) {
    this.bqTable = bqTable;
  }

  public BQTable getTablePointer() {
    return bqTable;
  }

  public abstract ImmutableList<ColumnSchema> getColumnSchemas();
}
