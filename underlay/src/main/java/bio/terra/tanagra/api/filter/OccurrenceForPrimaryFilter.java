package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class OccurrenceForPrimaryFilter extends EntityFilter {
  private final CriteriaOccurrence criteriaOccurrence;
  // At least one of the sub-filters must be not-null.
  private final @Nullable EntityFilter primarySubFilter;
  private final @Nullable EntityFilter criteriaSubFilter;

  public OccurrenceForPrimaryFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      Entity occurrenceEntity,
      @Nullable EntityFilter primarySubFilter,
      @Nullable EntityFilter criteriaSubFilter) {
    super(LoggerFactory.getLogger(OccurrenceForPrimaryFilter.class), underlay, occurrenceEntity);
    this.criteriaOccurrence = criteriaOccurrence;
    this.primarySubFilter = primarySubFilter;
    this.criteriaSubFilter = criteriaSubFilter;
  }

  @Override
  public List<Attribute> getFilterAttributes() {
    Attribute attribute =
        criteriaOccurrence
            .getOccurrencePrimaryRelationship(getOccurrenceEntity().getName())
            .getForeignKeyAttribute(getOccurrenceEntity());
    return attribute != null ? List.of(attribute) : List.of();
  }

  public CriteriaOccurrence getCriteriaOccurrence() {
    return criteriaOccurrence;
  }

  public Entity getOccurrenceEntity() {
    return getEntity();
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

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    OccurrenceForPrimaryFilter that = (OccurrenceForPrimaryFilter) o;
    return criteriaOccurrence.equals(that.criteriaOccurrence)
        && Objects.equals(primarySubFilter, that.primarySubFilter)
        && Objects.equals(criteriaSubFilter, that.criteriaSubFilter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), criteriaOccurrence, primarySubFilter, criteriaSubFilter);
  }
}
