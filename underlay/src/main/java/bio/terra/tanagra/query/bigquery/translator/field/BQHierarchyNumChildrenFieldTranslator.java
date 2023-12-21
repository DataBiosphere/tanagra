package bio.terra.tanagra.query.bigquery.translator.field;

import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.query.sql.translator.ApiFieldTranslator;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyNumChildrenFieldTranslator implements ApiFieldTranslator {
  private final HierarchyNumChildrenField hierarchyNumChildrenField;

  public BQHierarchyNumChildrenFieldTranslator(
      HierarchyNumChildrenField hierarchyNumChildrenField) {
    this.hierarchyNumChildrenField = hierarchyNumChildrenField;
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
    return List.of(SqlQueryField.of(getField()));
  }

  private SqlField getField() {
    ITEntityMain indexTable =
        hierarchyNumChildrenField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyNumChildrenField.getEntity().getName());
    return indexTable.getHierarchyNumChildrenField(
        hierarchyNumChildrenField.getHierarchy().getName());
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getField().getColumnName(), DataType.INT64));
  }
}
