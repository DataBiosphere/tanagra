package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFOutputUnfiltered;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTOutputUnfiltered;
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
    if (selectionData.size() > 1) {
      throw new InvalidQueryException("Modifiers are not supported for data features");
    }
    DTOutputUnfiltered.OutputUnfiltered outputUnfilteredSelectionData =
        deserializeData(selectionData.get(0).getPluginData());
    List<EntityOutput> entityOutputs = new ArrayList<>();
    outputUnfilteredSelectionData.getEntitiesList().stream()
        .forEach(
            outputEntityName ->
                entityOutputs.add(
                    EntityOutput.unfiltered(
                        outputEntityName.isEmpty()
                            ? underlay.getPrimaryEntity()
                            : underlay.getEntity(outputEntityName))));
    return entityOutputs;
  }

  @Override
  public CFOutputUnfiltered.OutputUnfiltered deserializeConfig() {
    return deserializeFromJson(
            criteriaSelector.getPluginConfig(), CFOutputUnfiltered.OutputUnfiltered.newBuilder())
        .build();
  }

  @Override
  public DTOutputUnfiltered.OutputUnfiltered deserializeData(String serialized) {
    return deserializeFromJson(serialized, DTOutputUnfiltered.OutputUnfiltered.newBuilder())
        .build();
  }
}
