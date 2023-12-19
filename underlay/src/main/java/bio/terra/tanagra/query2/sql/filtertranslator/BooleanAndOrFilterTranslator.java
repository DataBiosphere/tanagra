package bio.terra.tanagra.query2.sql.filtertranslator;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanAndOrFilterTranslator extends SqlFilterTranslator {
  private final BooleanAndOrFilter booleanAndOrFilter;

  public BooleanAndOrFilterTranslator(SqlTranslator sqlTranslator, BooleanAndOrFilter booleanAndOrFilter) {
    super(sqlTranslator);
    this.booleanAndOrFilter = booleanAndOrFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    List<String> subFilterSqls =
        booleanAndOrFilter.getSubFilters().stream()
            .map(
                subFilter ->
                    sqlTranslator.translator(subFilter).buildSql(sqlParams, tableAlias, idField))
            .collect(Collectors.toList());
    return sqlTranslator.booleanAndOrFilterSql(
        booleanAndOrFilter.getOperator(), subFilterSqls.toArray(new String[0]));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return booleanAndOrFilter
        .getSubFilters()
        .parallelStream()
        .filter(subFilter -> !sqlTranslator.translator(subFilter).isFilterOnAttribute(attribute))
        .findAny()
        .isEmpty();
  }
}
