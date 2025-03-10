package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITHierarchyAncestorDescendant extends IndexTable {
  public static final String TABLE_NAME = "HAD";
  private final String entity;
  private final String hierarchy;
  private final ImmutableList<ColumnSchema> columnSchemas;

  public ITHierarchyAncestorDescendant(
      NameHelper namer, SZBigQuery.IndexData bigQueryConfig, String entity, String hierarchy) {
    super(namer, bigQueryConfig);
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.columnSchemas =
        ImmutableList.copyOf(
            Arrays.stream(Column.values()).map(Column::getSchema).collect(Collectors.toList()));
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
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return columnSchemas;
  }

  public SqlField getAncestorField() {
    return SqlField.of(Column.ANCESTOR.getSchema().getColumnName());
  }

  public SqlField getDescendantField() {
    return SqlField.of(Column.DESCENDANT.getSchema().getColumnName());
  }

  public enum Column {
    ANCESTOR(new ColumnSchema("ancestor", DataType.INT64)),
    DESCENDANT(new ColumnSchema("descendant", DataType.INT64));

    private final ColumnSchema schema;

    Column(ColumnSchema schema) {
      this.schema = schema;
    }

    public ColumnSchema getSchema() {
      return schema;
    }
  }
}
