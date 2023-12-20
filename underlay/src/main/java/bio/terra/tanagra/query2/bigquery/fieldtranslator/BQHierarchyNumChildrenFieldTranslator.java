package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
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
  public List<SqlField> buildSqlFieldsForListSelect() {
    return buildSqlFields();
  }

  @Override
  public List<SqlField> buildSqlFieldsForCountSelect() {
    return buildSqlFields();
  }

  @Override
  public List<SqlField> buildSqlFieldsForOrderBy() {
    return buildSqlFields();
  }

  @Override
  public List<SqlField> buildSqlFieldsForGroupBy() {
    return buildSqlFields();
  }

  private List<SqlField> buildSqlFields() {
    return List.of(SqlField.of(getField(), null));
  }

  private FieldPointer getField() {
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
    return new ValueDisplay(sqlRowResult.get(getField().getColumnName(), Literal.DataType.INT64));
  }
}
