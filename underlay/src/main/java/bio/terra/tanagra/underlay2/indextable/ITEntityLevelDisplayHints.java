package bio.terra.tanagra.underlay2.indextable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.underlay2.NameHelper;
import com.google.common.collect.ImmutableList;
import java.util.*;
import java.util.stream.Collectors;

public final class ITEntityLevelDisplayHints extends IndexTable {
  public static final String TABLE_NAME = "ELDH";

  private final String entity;

  public ITEntityLevelDisplayHints(NameHelper namer, DataPointer dataPointer, String entity) {
    super(namer, dataPointer);
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
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    // Columns are static and don't depend on the entity.
    return ImmutableList.copyOf(
        Arrays.stream(Column.values())
            .map(column -> column.getSchema())
            .collect(Collectors.toList()));
  }

  public enum Column {
    ATTRIBUTE_NAME(new ColumnSchema("attribute_name", CellValue.SQLDataType.STRING, true)),
    MIN(new ColumnSchema("min", CellValue.SQLDataType.FLOAT)),
    MAX(new ColumnSchema("max", CellValue.SQLDataType.FLOAT)),
    ENUM_VALUE(new ColumnSchema("enum_value", CellValue.SQLDataType.INT64)),
    ENUM_DISPLAY(new ColumnSchema("enum_display", CellValue.SQLDataType.STRING)),
    ENUM_COUNT(new ColumnSchema("enum_count", CellValue.SQLDataType.INT64));

    private final ColumnSchema columnSchema;

    Column(ColumnSchema columnSchema) {
      this.columnSchema = columnSchema;
    }

    public ColumnSchema getSchema() {
      return columnSchema;
    }
  }
}
