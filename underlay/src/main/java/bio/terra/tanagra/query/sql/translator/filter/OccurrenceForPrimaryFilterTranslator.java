package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.List;
import java.util.Map;

public class OccurrenceForPrimaryFilterTranslator extends ApiFilterTranslator {
  private final OccurrenceForPrimaryFilter occurrenceForPrimaryFilter;

  public OccurrenceForPrimaryFilterTranslator(
      ApiTranslator apiTranslator,
      OccurrenceForPrimaryFilter occurrenceForPrimaryFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
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
            List.of(),
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
            List.of(),
            null,
            null);

    if (occurrenceForPrimaryFilter.hasPrimarySubFilter()
        && occurrenceForPrimaryFilter.hasCriteriaSubFilter()) {
      return apiTranslator
          .translator(
              new BooleanAndOrFilter(
                  BooleanAndOrFilter.LogicalOperator.AND,
                  List.of(primaryRelationshipFilter, criteriaRelationshipFilter)),
              attributeSwapFields)
          .buildSql(sqlParams, tableAlias);
    } else if (occurrenceForPrimaryFilter.hasPrimarySubFilter()) {
      return apiTranslator
          .translator(primaryRelationshipFilter, attributeSwapFields)
          .buildSql(sqlParams, tableAlias);
    } else if (occurrenceForPrimaryFilter.hasCriteriaSubFilter()) {
      return apiTranslator
          .translator(criteriaRelationshipFilter, attributeSwapFields)
          .buildSql(sqlParams, tableAlias);
    } else {
      throw new InvalidQueryException("At least one of the sub-filters must be not-null");
    }
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
