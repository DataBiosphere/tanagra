package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlGeneration;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.List;
import java.util.stream.Collectors;

public class BQBooleanAndOrFilterTranslator implements SqlFilterTranslator {
  private final BooleanAndOrFilter booleanAndOrFilter;

  public BQBooleanAndOrFilterTranslator(BooleanAndOrFilter booleanAndOrFilter) {
    this.booleanAndOrFilter = booleanAndOrFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    List<String> subFilterSqls =
        booleanAndOrFilter.getSubFilters().stream()
            .map(
                subFilter ->
                    BQTranslator.translator(subFilter).buildSql(sqlParams, tableAlias, idField))
            .collect(Collectors.toList());
    return SqlGeneration.booleanAndOrFilterSql(
        booleanAndOrFilter.getOperator(), subFilterSqls.toArray(new String[0]));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return booleanAndOrFilter
        .getSubFilters()
        .parallelStream()
        .filter(subFilter -> !BQTranslator.translator(subFilter).isFilterOnAttribute(attribute))
        .findAny()
        .isEmpty();
  }
}
