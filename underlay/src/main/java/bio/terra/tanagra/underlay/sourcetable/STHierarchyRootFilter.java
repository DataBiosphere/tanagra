package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.serialization.SZHierarchy;
import com.google.common.collect.ImmutableList;

public class STHierarchyRootFilter extends SourceTable {
  private final String entity;
  private final String hierarchy;
  private final ColumnSchema idColumnSchema;

  public STHierarchyRootFilter(BQTable bqTable, String entity, SZHierarchy szHierarchy) {
    super(bqTable);
    this.entity = entity;
    this.hierarchy = szHierarchy.name;
    this.idColumnSchema = new ColumnSchema(szHierarchy.rootIdFieldName, DataType.INT64);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return ImmutableList.of(idColumnSchema);
  }

  public String getEntity() {
    return entity;
  }

  public String getHierarchy() {
    return hierarchy;
  }

  public ColumnSchema getIdColumnSchema() {
    return idColumnSchema;
  }
}
