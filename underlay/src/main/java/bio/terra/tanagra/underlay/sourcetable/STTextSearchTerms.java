package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlTable;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;

public class STTextSearchTerms extends SourceTable {
  private final String entity;
  private final ColumnSchema idColumnSchema;
  private final ColumnSchema textColumnSchema;

  public STTextSearchTerms(SqlTable sqlTable, String entity, SZEntity.TextSearch szTextSearch) {
    super(sqlTable);
    this.entity = entity;
    this.idColumnSchema = new ColumnSchema(szTextSearch.idFieldName, DataType.INT64);
    this.textColumnSchema = new ColumnSchema(szTextSearch.textFieldName, DataType.STRING);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return ImmutableList.of(idColumnSchema, textColumnSchema);
  }

  public String getEntity() {
    return entity;
  }

  public SqlField getIdField() {
    return SqlField.of(getTablePointer(), idColumnSchema.getColumnName());
  }

  public SqlField getTextField() {
    return SqlField.of(getTablePointer(), textColumnSchema.getColumnName());
  }
}
