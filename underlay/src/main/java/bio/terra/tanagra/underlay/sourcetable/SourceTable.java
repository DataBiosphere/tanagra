package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlTable;
import com.google.common.collect.ImmutableList;

public abstract class SourceTable {
  protected SqlTable sqlTable;

  protected SourceTable(SqlTable sqlTable) {
    this.sqlTable = sqlTable;
  }

  public SqlTable getTablePointer() {
    return sqlTable;
  }

  public abstract ImmutableList<SqlColumnSchema> getColumnSchemas();
}
