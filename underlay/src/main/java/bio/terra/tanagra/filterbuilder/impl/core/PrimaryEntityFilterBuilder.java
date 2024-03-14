package bio.terra.tanagra.filterbuilder.impl.core;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.AttributeSchemaUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;

public class PrimaryEntityFilterBuilder extends FilterBuilder {
  public PrimaryEntityFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() != 1) {
      throw new InvalidConfigException("Primary entity filter builder does not support modifiers.");
    }
    CFAttribute.Attribute config = deserializeConfig();
    DTAttribute.Attribute data = deserializeData(selectionData.get(0).getPluginData());
    return AttributeSchemaUtils.buildForEntity(
        underlay,
        underlay.getPrimaryEntity(),
        underlay.getPrimaryEntity().getAttribute(config.getAttribute()),
        data);
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    return List.of(EntityOutput.unfiltered(underlay.getPrimaryEntity()));
  }

  @Override
  public CFAttribute.Attribute deserializeConfig() {
    return AttributeSchemaUtils.deserializeConfig(criteriaSelector.getPluginConfig());
  }

  @Override
  public DTAttribute.Attribute deserializeData(String serialized) {
    return AttributeSchemaUtils.deserializeData(serialized);
  }
}
