package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.api.query.ValueDisplay;
import java.util.List;

public interface SqlFieldTranslator {
  List<SqlField> buildSqlFieldsForListSelect();

  List<SqlField> buildSqlFieldsForCountSelect();

  List<SqlField> buildSqlFieldsForOrderBy();

  List<SqlField> buildSqlFieldsForGroupBy();

  ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult);
}
