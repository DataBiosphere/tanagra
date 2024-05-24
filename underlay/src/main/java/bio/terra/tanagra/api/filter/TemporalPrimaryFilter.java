package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.JoinOperator;
import bio.terra.tanagra.api.shared.ReducingOperator;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

public class TemporalPrimaryFilter extends EntityFilter {
  private final Underlay underlay;
  private final @Nullable ReducingOperator firstConditionQualifier;
  private final ImmutableList<EntityOutput> firstCondition;
  private final JoinOperator joinOperator;
  private final @Nullable Integer joinOperatorValue;
  private final @Nullable ReducingOperator secondConditionQualifier;
  private final ImmutableList<EntityOutput> secondCondition;

  public TemporalPrimaryFilter(
      Underlay underlay,
      @Nullable ReducingOperator firstConditionQualifier,
      List<EntityOutput> firstCondition,
      JoinOperator joinOperator,
      @Nullable Integer joinOperatorValue,
      @Nullable ReducingOperator secondConditionQualifier,
      List<EntityOutput> secondCondition) {
    this.underlay = underlay;
    this.firstConditionQualifier = firstConditionQualifier;
    this.firstCondition = ImmutableList.copyOf(firstCondition);
    this.joinOperator = joinOperator;
    this.joinOperatorValue = joinOperatorValue;
    this.secondConditionQualifier = secondConditionQualifier;
    this.secondCondition = ImmutableList.copyOf(secondCondition);
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  @Nullable
  public ReducingOperator getFirstConditionQualifier() {
    return firstConditionQualifier;
  }

  public List<EntityOutput> getFirstCondition() {
    return firstCondition;
  }

  public JoinOperator getJoinOperator() {
    return joinOperator;
  }

  @Nullable
  public Integer getJoinOperatorValue() {
    return joinOperatorValue;
  }

  @Nullable
  public ReducingOperator getSecondConditionQualifier() {
    return secondConditionQualifier;
  }

  public List<EntityOutput> getSecondCondition() {
    return secondCondition;
  }
}
