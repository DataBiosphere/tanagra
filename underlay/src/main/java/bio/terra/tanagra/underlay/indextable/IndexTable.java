package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;

public abstract class IndexTable {
  protected final NameHelper namer;
  protected final SZBigQuery.IndexData bigQueryConfig;

  protected IndexTable(NameHelper namer, SZBigQuery.IndexData bigQueryConfig) {
    this.namer = namer;
    this.bigQueryConfig = bigQueryConfig;
  }

  public abstract String getTableBaseName();

  public BQTable getTablePointer() {
    return new BQTable(
        bigQueryConfig.projectId,
        bigQueryConfig.datasetId,
        namer.getReservedTableName(getTableBaseName()));
  }

  public abstract ImmutableList<ColumnSchema> getColumnSchemas();
}
