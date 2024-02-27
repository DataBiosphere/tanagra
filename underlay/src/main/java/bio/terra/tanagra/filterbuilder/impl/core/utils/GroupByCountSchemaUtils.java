package bio.terra.tanagra.filterbuilder.impl.core.utils;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

public final class GroupByCountSchemaUtils {
  private GroupByCountSchemaUtils() {}

  public static CFPlaceholder.Placeholder deserializeConfig(String serialized) {
    return deserializeFromJson(serialized, CFPlaceholder.Placeholder.newBuilder()).build();
  }

  public static DTUnhintedValue.UnhintedValue deserializeData(String serialized) {
    return deserializeFromJson(serialized, DTUnhintedValue.UnhintedValue.newBuilder()).build();
  }

  public static Optional<Pair<CFPlaceholder.Placeholder, DTUnhintedValue.UnhintedValue>>
      getModifier(CriteriaSelector criteriaSelector, List<SelectionData> selectionData) {
    Optional<SelectionData> groupByCountSelectionData =
        selectionData.stream()
            .filter(
                modifierSelectionData ->
                    SZCorePlugin.UNHINTED_VALUE
                        .getIdInConfig()
                        .equals(
                            criteriaSelector
                                .getModifier(modifierSelectionData.getModifierName())
                                .getPlugin()))
            .findFirst();
    if (groupByCountSelectionData.isEmpty()) {
      return Optional.empty();
    }
    CFPlaceholder.Placeholder groupByModifierConfig =
        deserializeConfig(
            criteriaSelector
                .getModifier(groupByCountSelectionData.get().getModifierName())
                .getPluginConfig());
    DTUnhintedValue.UnhintedValue groupByModifierData =
        GroupByCountSchemaUtils.deserializeData(groupByCountSelectionData.get().getPluginData());
    return Optional.of(Pair.of(groupByModifierConfig, groupByModifierData));
  }

  public static BinaryOperator toBinaryOperator(
      DTUnhintedValue.UnhintedValue.ComparisonOperator comparisonOperator) {
    switch (comparisonOperator) {
      case COMPARISON_OPERATOR_EQUAL:
        return BinaryOperator.EQUALS;
      case COMPARISON_OPERATOR_LESS_THAN_EQUAL:
        return BinaryOperator.LESS_THAN_OR_EQUAL;
      case COMPARISON_OPERATOR_GREATER_THAN_EQUAL:
        return BinaryOperator.GREATER_THAN_OR_EQUAL;
      case COMPARISON_OPERATOR_BETWEEN:
      default:
        throw new SystemException(
            "Unsupported unhinted-value comparison operator: " + comparisonOperator);
    }
  }
}
