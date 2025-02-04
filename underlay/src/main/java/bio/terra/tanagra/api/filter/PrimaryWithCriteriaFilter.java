package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class PrimaryWithCriteriaFilter extends EntityFilter {
  private final CriteriaOccurrence criteriaOccurrence;
  private final EntityFilter criteriaSubFilter;
  private final ImmutableMap<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity;
  private final ImmutableMap<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity;
  private final @Nullable BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  public PrimaryWithCriteriaFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      EntityFilter criteriaSubFilter,
      @Nullable Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity,
      @Nullable Map<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity,
      @Nullable BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    super(
        LoggerFactory.getLogger(PrimaryWithCriteriaFilter.class),
        underlay,
        underlay.getPrimaryEntity());
    this.criteriaOccurrence = criteriaOccurrence;
    this.criteriaSubFilter = criteriaSubFilter;
    this.subFiltersPerOccurrenceEntity =
        subFiltersPerOccurrenceEntity == null
            ? ImmutableMap.of()
            : ImmutableMap.copyOf(subFiltersPerOccurrenceEntity);
    this.groupByAttributesPerOccurrenceEntity =
        groupByAttributesPerOccurrenceEntity == null
            ? ImmutableMap.of()
            : ImmutableMap.copyOf(groupByAttributesPerOccurrenceEntity);
    this.groupByCountOperator = groupByCountOperator;
    this.groupByCountValue = groupByCountValue;

    // one-time checks
    int numSubFiltersPerOccEntity = getNumSubFiltersPerOccEntity();
    this.subFiltersPerOccurrenceEntity.forEach(
        (occurrenceEntity, subFilters) -> {
          if (subFilters.size() != numSubFiltersPerOccEntity) {
            throw new InvalidQueryException(
                "There must be the same number of sub filters for each occurrence entity: "
                    + occurrenceEntity.getName());
          }
        });

    int numGroupByAttributesPerOccEntity = getNumGroupByAttributesPerOccEntity();
    this.groupByAttributesPerOccurrenceEntity.forEach(
        (occurrenceEntity, groupByAttributes) -> {
          if (groupByAttributes.size() != numGroupByAttributesPerOccEntity) {
            throw new InvalidQueryException(
                "There must be the same number of group by attributes for each occurrence entity: "
                    + occurrenceEntity.getName());
          }
        });
  }

  public CriteriaOccurrence getCriteriaOccurrence() {
    return criteriaOccurrence;
  }

  public EntityFilter getCriteriaSubFilter() {
    return criteriaSubFilter;
  }

  public int getNumSubFiltersPerOccEntity() {
    return getSubFilters(criteriaOccurrence.getOccurrenceEntities().get(0)).size();
  }

  public boolean hasSubFilters(Entity occurrenceEntity) {
    return subFiltersPerOccurrenceEntity.containsKey(occurrenceEntity);
  }

  public ImmutableList<EntityFilter> getSubFilters(Entity occurrenceEntity) {
    return hasSubFilters(occurrenceEntity)
        ? ImmutableList.copyOf(subFiltersPerOccurrenceEntity.get(occurrenceEntity))
        : ImmutableList.of();
  }

  public boolean hasGroupByModifier() {
    return groupByCountOperator != null;
  }

  public ImmutableList<Attribute> getGroupByAttributes(Entity occurrenceEntity) {
    return groupByAttributesPerOccurrenceEntity.containsKey(occurrenceEntity)
        ? ImmutableList.copyOf(groupByAttributesPerOccurrenceEntity.get(occurrenceEntity))
        : ImmutableList.of();
  }

  public int getNumGroupByAttributesPerOccEntity() {
    return getGroupByAttributes(criteriaOccurrence.getOccurrenceEntities().get(0)).size();
  }

  public boolean hasGroupByAttributes() {
    return getNumGroupByAttributesPerOccEntity() > 0;
  }

  @Nullable
  public BinaryOperator getGroupByCountOperator() {
    return groupByCountOperator;
  }

  @Nullable
  public Integer getGroupByCountValue() {
    return groupByCountValue;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    PrimaryWithCriteriaFilter that = (PrimaryWithCriteriaFilter) o;
    return criteriaOccurrence.equals(that.criteriaOccurrence)
        && Objects.equals(criteriaSubFilter, that.criteriaSubFilter)
        && Objects.equals(subFiltersPerOccurrenceEntity, that.subFiltersPerOccurrenceEntity)
        && Objects.equals(
            groupByAttributesPerOccurrenceEntity, that.groupByAttributesPerOccurrenceEntity)
        && groupByCountOperator == that.groupByCountOperator
        && Objects.equals(groupByCountValue, that.groupByCountValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        criteriaOccurrence,
        criteriaSubFilter,
        subFiltersPerOccurrenceEntity,
        groupByAttributesPerOccurrenceEntity,
        groupByCountOperator,
        groupByCountValue);
  }
}
