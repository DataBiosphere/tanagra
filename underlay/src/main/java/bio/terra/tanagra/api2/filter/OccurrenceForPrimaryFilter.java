package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.entitygroup.CriteriaOccurrence;

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
