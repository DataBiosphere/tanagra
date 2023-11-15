package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;

public class OccurrenceForPrimaryFilter extends RelationshipFilter {
  public OccurrenceForPrimaryFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      Entity occurrenceEntity,
      EntityFilter primaryEntitySubFilter) {
    super(
        underlay,
        criteriaOccurrence,
        occurrenceEntity,
        criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
        primaryEntitySubFilter,
        null,
        null,
        null);
  }
}
