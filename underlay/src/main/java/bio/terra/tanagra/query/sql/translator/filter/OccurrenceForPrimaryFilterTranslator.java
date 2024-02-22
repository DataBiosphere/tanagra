package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.List;

public class OccurrenceForPrimaryFilterTranslator extends ApiFilterTranslator {
  private final OccurrenceForPrimaryFilter occurrenceForPrimaryFilter;

  public OccurrenceForPrimaryFilterTranslator(
      ApiTranslator apiTranslator, OccurrenceForPrimaryFilter occurrenceForPrimaryFilter) {
    super(apiTranslator);
    this.occurrenceForPrimaryFilter = occurrenceForPrimaryFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    RelationshipFilter primaryRelationshipFilter =
        new RelationshipFilter(
            occurrenceForPrimaryFilter.getUnderlay(),
            occurrenceForPrimaryFilter.getCriteriaOccurrence(),
            occurrenceForPrimaryFilter.getOccurrenceEntity(),
            occurrenceForPrimaryFilter
                .getCriteriaOccurrence()
                .getOccurrencePrimaryRelationship(
                    occurrenceForPrimaryFilter.getOccurrenceEntity().getName()),
            occurrenceForPrimaryFilter.getPrimarySubFilter(),
            null,
            null,
            null);

    RelationshipFilter criteriaRelationshipFilter =
        new RelationshipFilter(
            occurrenceForPrimaryFilter.getUnderlay(),
            occurrenceForPrimaryFilter.getCriteriaOccurrence(),
            occurrenceForPrimaryFilter.getOccurrenceEntity(),
            occurrenceForPrimaryFilter
                .getCriteriaOccurrence()
                .getOccurrenceCriteriaRelationship(
                    occurrenceForPrimaryFilter.getOccurrenceEntity().getName()),
            occurrenceForPrimaryFilter.getCriteriaSubFilter(),
            null,
            null,
            null);

    if (occurrenceForPrimaryFilter.hasPrimarySubFilter()
        && occurrenceForPrimaryFilter.hasCriteriaSubFilter()) {
      return apiTranslator
          .translator(
              new BooleanAndOrFilter(
                  BooleanAndOrFilter.LogicalOperator.AND,
                  List.of(primaryRelationshipFilter, criteriaRelationshipFilter)))
          .buildSql(sqlParams, tableAlias);
    } else if (occurrenceForPrimaryFilter.hasPrimarySubFilter()) {
      return apiTranslator.translator(primaryRelationshipFilter).buildSql(sqlParams, tableAlias);
    } else if (occurrenceForPrimaryFilter.hasCriteriaSubFilter()) {
      return apiTranslator.translator(criteriaRelationshipFilter).buildSql(sqlParams, tableAlias);
    } else {
      throw new InvalidQueryException("At least one of the sub-filters must be not-null");
    }
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
