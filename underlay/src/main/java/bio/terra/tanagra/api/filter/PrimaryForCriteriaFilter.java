package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class PrimaryForCriteriaFilter extends EntityFilter {
  private final Underlay underlay;
  private final CriteriaOccurrence criteriaOccurrence;
  private final EntityFilter criteriaEntitySubFilter;
  private final ImmutableMap<String, EntityFilter> occurrenceEntitySubFilters;
  private final ImmutableMap<String, Attribute> occurrenceEntityAttributeCountDistincts;
  private final @Nullable BinaryFilterVariable.BinaryOperator countOperator;
  private final @Nullable Integer countValue;

  public PrimaryForCriteriaFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      EntityFilter criteriaEntitySubFilter,
      @Nullable Map<String, EntityFilter> occurrenceEntitySubFilters,
      @Nullable Map<String, Attribute> occurrenceEntityAttributeCountDistincts,
      @Nullable BinaryFilterVariable.BinaryOperator countOperator,
      @Nullable Integer countValue) {
    this.underlay = underlay;
    this.criteriaOccurrence = criteriaOccurrence;
    this.criteriaEntitySubFilter = criteriaEntitySubFilter;
    this.occurrenceEntitySubFilters =
        occurrenceEntitySubFilters == null
            ? ImmutableMap.of()
            : ImmutableMap.copyOf(occurrenceEntitySubFilters);
    this.occurrenceEntityAttributeCountDistincts =
        occurrenceEntityAttributeCountDistincts == null
            ? ImmutableMap.of()
            : ImmutableMap.copyOf(occurrenceEntityAttributeCountDistincts);
    this.countOperator = countOperator;
    this.countValue = countValue;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    if (!includesModifiers()) {
      // Use a single relationship filter on primary-criteria relationship.
      return new RelationshipFilter(
              underlay,
              criteriaOccurrence,
              criteriaOccurrence.getPrimaryEntity(),
              criteriaOccurrence.getPrimaryCriteriaRelationship(),
              criteriaEntitySubFilter,
              null,
              null,
              null)
          .getFilterVariable(entityTableVar, tableVars);
    } else if (criteriaOccurrence.getOccurrenceEntities().size() == 1) {
      // TODO: Use 2 nested relationship filters on primary-occurrence, occurrence-criteria
      // relationships.
      return null;
    } else {
      // TODO: Use 2x nested relationship filters on primary-occurrence, occurrence-criteria
      // relationships, one pair per occurrence entity.
      return null;
    }
  }

  private boolean includesModifiers() {
    return !(occurrenceEntitySubFilters.isEmpty()
        && occurrenceEntityAttributeCountDistincts.isEmpty()
        && countOperator == null
        && countValue == null);
  }
}
