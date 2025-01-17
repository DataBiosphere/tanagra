package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.proto.criteriaselector.configschema.CFSurvey;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFSurvey.Survey.EntityGroupConfig;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTSurvey;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

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
    return StringUtils.isEmpty(serialized)
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
        .filter(id -> id.hasKey() && id.getKey().hasInt64Key())
        .map(
            selectedId ->
                new SelectionItem(
                    keyToLiteral(selectedId.getKey()),
                    new SelectionGroup(
                        selectedId.getEntityGroup(),
                        selectedId.hasValueData() ? selectedId.getValueData() : null)))
        .toList();
  }
}
