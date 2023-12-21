package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlRowResult;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyPathFieldTranslator implements SqlFieldTranslator {
  private final HierarchyPathField hierarchyPathField;

  public BQHierarchyPathFieldTranslator(HierarchyPathField hierarchyPathField) {
    this.hierarchyPathField = hierarchyPathField;
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
        hierarchyPathField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyPathField.getEntity().getName());
    return indexTable.getHierarchyPathField(hierarchyPathField.getHierarchy().getName());
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getField().getColumnName(), Literal.DataType.STRING));
  }
}
