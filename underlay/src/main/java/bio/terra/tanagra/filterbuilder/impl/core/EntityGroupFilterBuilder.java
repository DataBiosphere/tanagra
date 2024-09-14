package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFEntityGroup.EntityGroup.EntityGroupConfig;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityGroupFilterBuilder
    extends EntityGroupFilterBuilderBase<CFEntityGroup.EntityGroup, DTEntityGroup.EntityGroup> {
  public EntityGroupFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public CFEntityGroup.EntityGroup deserializeConfig() {
    return deserializeFromJsonOrProtoBytes(
            criteriaSelector.getPluginConfig(), CFEntityGroup.EntityGroup.newBuilder())
        .build();
  }

  @Override
  public DTEntityGroup.EntityGroup deserializeData(String serialized) {
    return (serialized == null || serialized.isEmpty())
        ? null
        : deserializeFromJsonOrProtoBytes(serialized, DTEntityGroup.EntityGroup.newBuilder())
            .build();
  }

  @Override
  protected List<String> entityGroupIds() {
    return deserializeConfig().getClassificationEntityGroupsList().stream()
        .map(EntityGroupConfig::getId)
        .collect(Collectors.toList());
  }

  @Override
  protected Map<Literal, String> selectedIdsAndEntityGroups(String serializedSelectionData) {
    Map<Literal, String> idsAndEntityGroups = new HashMap<>();
    DTEntityGroup.EntityGroup selectionData = deserializeData(serializedSelectionData);
    if (selectionData != null) {
      for (DTEntityGroup.EntityGroup.Selection selectedId : selectionData.getSelectedList()) {
        if (selectedId.hasKey()) {
          idsAndEntityGroups.put(
              Literal.forInt64(selectedId.getKey().getInt64Key()), selectedId.getEntityGroup());
        }
      }
    }
    return idsAndEntityGroups;
  }

  @Override
  protected ValueDataOuterClass.ValueData valueData(String serializedSelectionData) {
    DTEntityGroup.EntityGroup selectionData = deserializeData(serializedSelectionData);
    return selectionData.hasValueData() ? selectionData.getValueData() : null;
  }
}
