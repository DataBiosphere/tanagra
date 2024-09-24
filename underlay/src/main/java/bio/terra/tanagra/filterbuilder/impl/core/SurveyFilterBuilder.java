package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFSurvey;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFSurvey.Survey.EntityGroupConfig;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTSurvey;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import java.util.List;
import java.util.stream.Collectors;

public class SurveyFilterBuilder
    extends EntityGroupFilterBuilderBase<CFSurvey.Survey, DTSurvey.Survey> {
  public SurveyFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public CFSurvey.Survey deserializeConfig() {
    return deserializeFromJsonOrProtoBytes(
            criteriaSelector.getPluginConfig(), CFSurvey.Survey.newBuilder())
        .build();
  }

  @Override
  public DTSurvey.Survey deserializeData(String serialized) {
    return (serialized == null || serialized.isEmpty())
        ? null
        : deserializeFromJsonOrProtoBytes(serialized, DTSurvey.Survey.newBuilder()).build();
  }

  @Override
  protected List<String> entityGroupIds() {
    return deserializeConfig().getEntityGroupsList().stream()
        .map(EntityGroupConfig::getId)
        .collect(Collectors.toList());
  }

  @Override
  protected List<SelectionItem> selectedIdsAndGroups(String serializedSelectionData) {
    DTSurvey.Survey selectionData = deserializeData(serializedSelectionData);
    if (selectionData == null) {
      return List.of();
    }

    return selectionData.getSelectedList().stream()
        .filter(id -> id.hasKey())
        .map(
            selectedId ->
                new SelectionItem(
                    Literal.forInt64(selectedId.getKey().getInt64Key()),
                    new SelectionGroup(
                        selectedId.getEntityGroup(),
                        selectedId.hasValueData() ? selectedId.getValueData() : null)))
        .toList();
  }
}
