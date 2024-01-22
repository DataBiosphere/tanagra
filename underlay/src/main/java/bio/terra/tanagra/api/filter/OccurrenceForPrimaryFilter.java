package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import javax.annotation.Nullable;

public class OccurrenceForPrimaryFilter extends EntityFilter {
  private final Underlay underlay;
  private final CriteriaOccurrence criteriaOccurrence;
  private final Entity occurrenceEntity;
  private final @Nullable EntityFilter primarySubFilter;

  public OccurrenceForPrimaryFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      Entity occurrenceEntity,
      @Nullable EntityFilter primarySubFilter) {
    this.underlay = underlay;
    this.criteriaOccurrence = criteriaOccurrence;
    this.occurrenceEntity = occurrenceEntity;
    this.primarySubFilter = primarySubFilter;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public CriteriaOccurrence getCriteriaOccurrence() {
    return criteriaOccurrence;
  }

  public Entity getOccurrenceEntity() {
    return occurrenceEntity;
  }

  public @Nullable EntityFilter getPrimarySubFilter() {
    return primarySubFilter;
  }
}
