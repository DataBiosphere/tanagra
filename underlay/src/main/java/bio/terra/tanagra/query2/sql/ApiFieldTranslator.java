package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.api.shared.ValueDisplay;
import java.util.List;

public interface ApiFieldTranslator {
  List<SqlQueryField> buildSqlFieldsForListSelect();

  List<SqlQueryField> buildSqlFieldsForCountSelect();

  List<SqlQueryField> buildSqlFieldsForOrderBy();

  List<SqlQueryField> buildSqlFieldsForGroupBy();

  ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult);
}
