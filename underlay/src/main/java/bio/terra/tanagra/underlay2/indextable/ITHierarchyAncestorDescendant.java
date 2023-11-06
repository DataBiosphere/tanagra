package bio.terra.tanagra.underlay2.indextable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.NameHelper;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITHierarchyAncestorDescendant extends IndexTable {
  public static final String TABLE_NAME = "HAD";
  private final String entity;
  private final String hierarchy;

  public ITHierarchyAncestorDescendant(
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

  public FieldPointer getAncestorField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.ANCESTOR.getSchema().getColumnName())
        .build();
  }

  public FieldPointer getDescendantField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.DESCENDANT.getSchema().getColumnName())
        .build();
  }

  public enum Column {
    ANCESTOR(new ColumnSchema("ancestor", CellValue.SQLDataType.INT64)),
    DESCENDANT(new ColumnSchema("descendant", CellValue.SQLDataType.INT64));

    private final ColumnSchema schema;

    Column(ColumnSchema schema) {
      this.schema = schema;
    }

    public ColumnSchema getSchema() {
      return schema;
    }
  }
}
