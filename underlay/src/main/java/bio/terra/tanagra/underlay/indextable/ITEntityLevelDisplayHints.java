package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITEntityLevelDisplayHints extends IndexTable {
  public static final String TABLE_NAME = "ELDH";
  private final String entity;
  private final ImmutableList<ColumnSchema> columnSchemas;
  private final ImmutableList<String> orderBys;

  public ITEntityLevelDisplayHints(
      NameHelper namer, SZBigQuery.IndexData bigQueryConfig, String entity) {
    super(namer, bigQueryConfig);
    this.entity = entity;
    this.columnSchemas =
        ImmutableList.copyOf(
            Arrays.stream(Column.values()).map(Column::getSchema).collect(Collectors.toList()));
    this.orderBys =
        ImmutableList.of(
            Column.ATTRIBUTE_NAME.getSchema().getColumnName(),
            Column.ENUM_VALUE.getSchema().getColumnName(),
            Column.ENUM_DISPLAY.getSchema().getColumnName(),
            Column.ENUM_COUNT.getSchema().getColumnName() + " DESC");
  }

  public String getEntity() {
    return entity;
  }

  @Override
  public String getTableBaseName() {
    return TABLE_NAME + "_" + entity;
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return columnSchemas;
  }

  public ImmutableList<String> getOrderBys() {
    return orderBys;
  }

  public enum Column {
    ATTRIBUTE_NAME(new ColumnSchema("attribute_name", DataType.STRING, true)),
    MIN(new ColumnSchema("min", DataType.DOUBLE)),
    MAX(new ColumnSchema("max", DataType.DOUBLE)),
    ENUM_VALUE(new ColumnSchema("enum_value", DataType.INT64)),
    ENUM_DISPLAY(new ColumnSchema("enum_display", DataType.STRING)),
    ENUM_COUNT(new ColumnSchema("enum_count", DataType.INT64));

    private final ColumnSchema columnSchema;

    Column(ColumnSchema columnSchema) {
      this.columnSchema = columnSchema;
    }

    public ColumnSchema getSchema() {
      return columnSchema;
    }
  }
}
