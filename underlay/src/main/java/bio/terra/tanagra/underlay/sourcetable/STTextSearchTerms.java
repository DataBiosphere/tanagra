package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;

public class STTextSearchTerms extends SourceTable {
  private final String entity;
  private final ColumnSchema idColumnSchema;
  private final ColumnSchema textColumnSchema;

  public STTextSearchTerms(
      TablePointer tablePointer, String entity, SZEntity.TextSearch szTextSearch) {
    super(tablePointer);
    this.entity = entity;
    this.idColumnSchema = new ColumnSchema(szTextSearch.idFieldName, CellValue.SQLDataType.INT64);
    this.textColumnSchema =
        new ColumnSchema(szTextSearch.textFieldName, CellValue.SQLDataType.STRING);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return ImmutableList.of(idColumnSchema, textColumnSchema);
  }

  public String getEntity() {
    return entity;
  }

  public FieldPointer getIdField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(idColumnSchema.getColumnName())
        .build();
  }

  public FieldPointer getTextField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(textColumnSchema.getColumnName())
        .build();
  }
}
