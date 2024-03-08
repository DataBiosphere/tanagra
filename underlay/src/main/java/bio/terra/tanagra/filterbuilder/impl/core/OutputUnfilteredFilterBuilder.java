package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;

public class OutputUnfilteredFilterBuilder extends FilterBuilder {
  public OutputUnfilteredFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    throw new UnsupportedOperationException("This filter builder only supports data features");
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    CFPlaceholder.Placeholder config = deserializeConfig();
    List<EntityOutput> entityOutputs = new ArrayList<>();
    config.getOutputUnfilteredEntitiesList().stream()
        .forEach(
            outputEntityName ->
                entityOutputs.add(EntityOutput.unfiltered(underlay.getEntity(outputEntityName))));
    return entityOutputs;
  }

  @Override
  public CFPlaceholder.Placeholder deserializeConfig() {
    return deserializeFromJson(
            criteriaSelector.getPluginConfig(), CFPlaceholder.Placeholder.newBuilder())
        .build();
  }

  @Override
  public Object deserializeData(String serialized) {
    throw new InvalidQueryException("This filter builder does not support plugin data");
  }
}
