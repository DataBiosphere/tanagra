package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlQueryField;
import bio.terra.tanagra.query2.sql.SqlRowResult;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyPathFieldTranslator implements SqlFieldTranslator {
  private final HierarchyPathField hierarchyPathField;

  public BQHierarchyPathFieldTranslator(HierarchyPathField hierarchyPathField) {
    this.hierarchyPathField = hierarchyPathField;
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForListSelect() {
    return buildSqlFields();
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForCountSelect() {
    return buildSqlFields();
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForOrderBy() {
    return buildSqlFields();
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForGroupBy() {
    return buildSqlFields();
  }

  private List<SqlQueryField> buildSqlFields() {
    return List.of(SqlQueryField.of(getField(), null));
  }

  private SqlField getField() {
    ITEntityMain indexTable =
        hierarchyPathField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyPathField.getEntity().getName());
    return indexTable.getHierarchyPathField(hierarchyPathField.getHierarchy().getName());
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getField().getColumnName(), DataType.STRING));
  }
}
