package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFSurvey;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFSurvey.Survey.EntityGroupConfig;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTSurvey;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  protected Map<Literal, String> selectedIdsAndEntityGroups(String serializedSelectionData) {
    Map<Literal, String> idsAndEntityGroups = new HashMap<>();
    DTSurvey.Survey selectionData = deserializeData(serializedSelectionData);
    for (DTSurvey.Survey.Selection selectedId : selectionData.getSelectedList()) {
      if (selectedId.hasKey()) {
        idsAndEntityGroups.put(
            Literal.forInt64(selectedId.getKey().getInt64Key()), selectedId.getEntityGroup());
      }
    }
    return idsAndEntityGroups;
  }

  @Override
  protected ValueDataOuterClass.ValueData valueData(String serializedSelectionData) {
    DTSurvey.Survey selectionData = deserializeData(serializedSelectionData);
    return selectionData.hasValueData() ? selectionData.getValueData() : null;
  }
}
