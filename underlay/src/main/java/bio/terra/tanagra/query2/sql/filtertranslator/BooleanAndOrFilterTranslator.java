package bio.terra.tanagra.query2.sql.filtertranslator;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.query2.sql.ApiFilterTranslator;
import bio.terra.tanagra.query2.sql.ApiTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanAndOrFilterTranslator extends ApiFilterTranslator {
  private final BooleanAndOrFilter booleanAndOrFilter;

  public BooleanAndOrFilterTranslator(
      ApiTranslator apiTranslator, BooleanAndOrFilter booleanAndOrFilter) {
    super(apiTranslator);
    this.booleanAndOrFilter = booleanAndOrFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    List<String> subFilterSqls =
        booleanAndOrFilter.getSubFilters().stream()
            .map(subFilter -> apiTranslator.translator(subFilter).buildSql(sqlParams, tableAlias))
            .collect(Collectors.toList());
    return apiTranslator.booleanAndOrFilterSql(
        booleanAndOrFilter.getOperator(), subFilterSqls.toArray(new String[0]));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return booleanAndOrFilter
        .getSubFilters()
        .parallelStream()
        .filter(subFilter -> !apiTranslator.translator(subFilter).isFilterOnAttribute(attribute))
        .findAny()
        .isEmpty();
  }
}
