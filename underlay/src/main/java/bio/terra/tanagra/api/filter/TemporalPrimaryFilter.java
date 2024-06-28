package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.JoinOperator;
import bio.terra.tanagra.api.shared.ReducingOperator;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.List;

public class TemporalPrimaryFilter extends EntityFilter {
  private final Underlay underlay;
  private final @Nullable ReducingOperator firstConditionReducingOperator;
  private final ImmutableList<EntityOutput> firstCondition;
  private final JoinOperator joinOperator;
  private final @Nullable Integer joinOperatorValue;
  private final @Nullable ReducingOperator secondConditionReducingOperator;
  private final ImmutableList<EntityOutput> secondCondition;

  public TemporalPrimaryFilter(
      Underlay underlay,
      @Nullable ReducingOperator firstConditionReducingOperator,
      List<EntityOutput> firstCondition,
      JoinOperator joinOperator,
      @Nullable Integer joinOperatorValue,
      @Nullable ReducingOperator secondConditionReducingOperator,
      List<EntityOutput> secondCondition) {
    this.underlay = underlay;
    this.firstConditionReducingOperator = firstConditionReducingOperator;
    this.firstCondition = ImmutableList.copyOf(firstCondition);
    this.joinOperator = joinOperator;
    this.joinOperatorValue = joinOperatorValue;
    this.secondConditionReducingOperator = secondConditionReducingOperator;
    this.secondCondition = ImmutableList.copyOf(secondCondition);
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  @Nullable
  public ReducingOperator getFirstConditionReducingOperator() {
    return firstConditionReducingOperator;
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
  public ReducingOperator getSecondConditionReducingOperator() {
    return secondConditionReducingOperator;
  }

  public List<EntityOutput> getSecondCondition() {
    return secondCondition;
  }
}
