package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITHierarchyAncestorDescendant extends IndexTable {
  public static final String TABLE_NAME = "HAD";
  private final String entity;
  private final String hierarchy;

  public ITHierarchyAncestorDescendant(
      NameHelper namer, SZBigQuery.IndexData bigQueryConfig, String entity, String hierarchy) {
    super(namer, bigQueryConfig);
    this.entity = entity;
    this.hierarchy = hierarchy;
  }

  public String getEntity() {
    return entity;
  }

  public String getHierarchy() {
    return hierarchy;
  }

  @Override
  public String getTableBaseName() {
    return TABLE_NAME + "_" + entity + "_" + hierarchy;
  }

  @Override
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    // Columns are static and don't depend on the entity or hierarchy.
    return ImmutableList.copyOf(
        Arrays.stream(Column.values())
            .map(column -> column.getSchema())
            .collect(Collectors.toList()));
  }

  public SqlField getAncestorField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.ANCESTOR.getSchema().getColumnName())
        .build();
  }

  public SqlField getDescendantField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.DESCENDANT.getSchema().getColumnName())
        .build();
  }

  public enum Column {
    ANCESTOR(new SqlColumnSchema("ancestor", DataType.INT64)),
    DESCENDANT(new SqlColumnSchema("descendant", DataType.INT64));

    private final SqlColumnSchema schema;

    Column(SqlColumnSchema schema) {
      this.schema = schema;
    }

    public SqlColumnSchema getSchema() {
      return schema;
    }
  }
}
