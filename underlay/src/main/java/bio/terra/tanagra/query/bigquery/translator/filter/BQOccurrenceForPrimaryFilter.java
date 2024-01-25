package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;

public class BQOccurrenceForPrimaryFilter extends ApiFilterTranslator {
  private final OccurrenceForPrimaryFilter occurrenceForPrimaryFilter;

  public BQOccurrenceForPrimaryFilter(
      ApiTranslator apiTranslator, OccurrenceForPrimaryFilter occurrenceForPrimaryFilter) {
    super(apiTranslator);
    this.occurrenceForPrimaryFilter = occurrenceForPrimaryFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    RelationshipFilter relationshipFilter =
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
    return apiTranslator.translator(relationshipFilter).buildSql(sqlParams, tableAlias);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
