package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import javax.annotation.Nullable;

public class OccurrenceForPrimaryFilter extends EntityFilter {
  private final Underlay underlay;
  private final CriteriaOccurrence criteriaOccurrence;
  private final Entity occurrenceEntity;
  // At least one of the sub-filters must be not-null.
  private final @Nullable EntityFilter primarySubFilter;
  private final @Nullable EntityFilter criteriaSubFilter;

  public OccurrenceForPrimaryFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      Entity occurrenceEntity,
      @Nullable EntityFilter primarySubFilter,
      @Nullable EntityFilter criteriaSubFilter) {
    this.underlay = underlay;
    this.criteriaOccurrence = criteriaOccurrence;
    this.occurrenceEntity = occurrenceEntity;
    this.primarySubFilter = primarySubFilter;
    this.criteriaSubFilter = criteriaSubFilter;
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

  public boolean hasPrimarySubFilter() {
    return primarySubFilter != null;
  }

  public @Nullable EntityFilter getPrimarySubFilter() {
    return primarySubFilter;
  }

  public boolean hasCriteriaSubFilter() {
    return criteriaSubFilter != null;
  }

  public @Nullable EntityFilter getCriteriaSubFilter() {
    return criteriaSubFilter;
  }
}
