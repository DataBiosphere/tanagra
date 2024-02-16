package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public class PrimaryWithCriteriaFilter extends EntityFilter {
  private final Underlay underlay;
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
    this.underlay = underlay;
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
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public CriteriaOccurrence getCriteriaOccurrence() {
    return criteriaOccurrence;
  }

  public EntityFilter getCriteriaSubFilter() {
    return criteriaSubFilter;
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

  public int getNumGroupByAttributes() {
    int numGroupByAttributes =
        getGroupByAttributes(criteriaOccurrence.getOccurrenceEntities().get(0)).size();
    groupByAttributesPerOccurrenceEntity.entrySet().stream()
        .forEach(
            entry -> {
              Entity occurrenceEntity = entry.getKey();
              List<Attribute> groupByAttributes = entry.getValue();
              if (groupByAttributes.size() != numGroupByAttributes) {
                throw new InvalidQueryException(
                    "There must be the same number of group by attributes for each occurrence entity: "
                        + occurrenceEntity.getName());
              }
            });
    return numGroupByAttributes;
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
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrimaryWithCriteriaFilter that = (PrimaryWithCriteriaFilter) o;
    return underlay.equals(that.underlay)
        && criteriaOccurrence.equals(that.criteriaOccurrence)
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
        underlay,
        criteriaOccurrence,
        criteriaSubFilter,
        subFiltersPerOccurrenceEntity,
        groupByAttributesPerOccurrenceEntity,
        groupByCountOperator,
        groupByCountValue);
  }
}
