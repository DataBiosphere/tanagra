package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlTable;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;

public class STTextSearchTerms extends SourceTable {
  private final String entity;
  private final SqlColumnSchema idColumnSchema;
  private final SqlColumnSchema textColumnSchema;

  public STTextSearchTerms(SqlTable sqlTable, String entity, SZEntity.TextSearch szTextSearch) {
    super(sqlTable);
    this.entity = entity;
    this.idColumnSchema = new SqlColumnSchema(szTextSearch.idFieldName, DataType.INT64);
    this.textColumnSchema = new SqlColumnSchema(szTextSearch.textFieldName, DataType.STRING);
  }

  @Override
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    return ImmutableList.of(idColumnSchema, textColumnSchema);
  }

  public String getEntity() {
    return entity;
  }

  public SqlField getIdField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(idColumnSchema.getColumnName())
        .build();
  }

  public SqlField getTextField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(textColumnSchema.getColumnName())
        .build();
  }
}
