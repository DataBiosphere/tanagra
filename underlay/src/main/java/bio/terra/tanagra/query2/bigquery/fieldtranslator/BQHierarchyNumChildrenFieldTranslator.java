package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlQueryField;
import bio.terra.tanagra.query2.sql.SqlRowResult;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyNumChildrenFieldTranslator implements SqlFieldTranslator {
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
    return List.of(SqlQueryField.of(getField(), null));
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
