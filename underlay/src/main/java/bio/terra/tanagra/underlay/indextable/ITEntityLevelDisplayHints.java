package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITEntityLevelDisplayHints extends IndexTable {
  public static final String TABLE_NAME = "ELDH";

  private final String entity;

  public ITEntityLevelDisplayHints(
      NameHelper namer, SZBigQuery.IndexData bigQueryConfig, String entity) {
    super(namer, bigQueryConfig);
    this.entity = entity;
  }

  public String getEntity() {
    return entity;
  }

  @Override
  public String getTableBaseName() {
    return TABLE_NAME + "_" + entity;
  }

  @Override
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    // Columns are static and don't depend on the entity.
    return ImmutableList.copyOf(
        Arrays.stream(Column.values())
            .map(column -> column.getSchema())
            .collect(Collectors.toList()));
  }

  public enum Column {
    ATTRIBUTE_NAME(new SqlColumnSchema("attribute_name", DataType.STRING, true)),
    MIN(new SqlColumnSchema("min", DataType.DOUBLE)),
    MAX(new SqlColumnSchema("max", DataType.DOUBLE)),
    ENUM_VALUE(new SqlColumnSchema("enum_value", DataType.INT64)),
    ENUM_DISPLAY(new SqlColumnSchema("enum_display", DataType.STRING)),
    ENUM_COUNT(new SqlColumnSchema("enum_count", DataType.INT64));

    private final SqlColumnSchema columnSchema;

    Column(SqlColumnSchema columnSchema) {
      this.columnSchema = columnSchema;
    }

    public SqlColumnSchema getSchema() {
      return columnSchema;
    }
  }
}
