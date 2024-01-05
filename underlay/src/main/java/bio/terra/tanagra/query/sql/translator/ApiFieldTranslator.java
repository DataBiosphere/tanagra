package bio.terra.tanagra.query.sql.translator;

import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlRowResult;
import java.util.List;

public interface ApiFieldTranslator {
  List<SqlQueryField> buildSqlFieldsForListSelect();

  List<SqlQueryField> buildSqlFieldsForCountSelect();

  List<SqlQueryField> buildSqlFieldsForOrderBy();

  List<SqlQueryField> buildSqlFieldsForGroupBy();

  ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult);
}
