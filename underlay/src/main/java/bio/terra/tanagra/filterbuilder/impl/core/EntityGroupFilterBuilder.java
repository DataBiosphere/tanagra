package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import java.util.List;
import java.util.stream.Stream;

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
    CFEntityGroup.EntityGroup config = deserializeConfig();
    return Stream.concat(
            config.getClassificationEntityGroupsList().stream(),
            config.getGroupingEntityGroupsList().stream())
        .map(CFEntityGroup.EntityGroup.EntityGroupConfig::getId)
        .toList();
  }

  @Override
  protected List<SelectionItem> selectedIdsAndGroups(String serializedSelectionData) {
    DTEntityGroup.EntityGroup selectionData = deserializeData(serializedSelectionData);
    if (selectionData == null) {
      return List.of();
    }

    return selectionData.getSelectedList().stream()
        .filter(id -> id.hasKey() && id.getKey().hasInt64Key())
        .map(
            selectedId -> {
              ValueDataOuterClass.ValueData valueData =
                  selectedId.hasValueData() ? selectedId.getValueData() : null;
              if (valueData == null) {
                // For backwards compatability, put selection level value data into the individual
                // IDs. This will only be the case when there is a single selection and any updates
                // to the data will migrate the value data permanently.
                valueData = selectionData.hasValueData() ? selectionData.getValueData() : null;
              }

              return new SelectionItem(
                  keyToLiteral(selectedId.getKey()),
                  new SelectionGroup(selectedId.getEntityGroup(), valueData));
            })
        .toList();
  }
}
