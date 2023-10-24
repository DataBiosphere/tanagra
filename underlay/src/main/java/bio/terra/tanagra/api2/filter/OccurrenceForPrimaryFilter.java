package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.entitygroup.CriteriaOccurrence;

public class OccurrenceForPrimaryFilter extends RelationshipFilter {
  public OccurrenceForPrimaryFilter(
      CriteriaOccurrence criteriaOccurrence,
      Entity occurrenceEntity,
      EntityFilter primaryEntitySubFilter) {
    super(
        occurrenceEntity,
        criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
        primaryEntitySubFilter,
        null,
        null,
        null);
  }
}
