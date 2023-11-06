package bio.terra.tanagra.underlay2.indextable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.NameHelper;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITHierarchyChildParent extends IndexTable {
  private static final String TABLE_NAME = "HCP";
  private final String entity;
  private final String hierarchy;

  public ITHierarchyChildParent(
      NameHelper namer, DataPointer dataPointer, String entity, String hierarchy) {
    super(namer, dataPointer);
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
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    // Columns are static and don't depend on the entity or hierarchy.
    return ImmutableList.copyOf(
        Arrays.stream(Column.values())
            .map(column -> column.getSchema())
            .collect(Collectors.toList()));
  }

  public FieldPointer getChildField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.CHILD.getSchema().getColumnName())
        .build();
  }

  public FieldPointer getParentField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.PARENT.getSchema().getColumnName())
        .build();
  }

  public enum Column {
    CHILD(new ColumnSchema("child", CellValue.SQLDataType.INT64)),
    PARENT(new ColumnSchema("parent", CellValue.SQLDataType.INT64));

    private final ColumnSchema schema;

    Column(ColumnSchema schema) {
      this.schema = schema;
    }

    public ColumnSchema getSchema() {
      return schema;
    }
  }
}
