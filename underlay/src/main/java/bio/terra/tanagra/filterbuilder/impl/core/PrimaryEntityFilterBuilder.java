package bio.terra.tanagra.filterbuilder.impl.core;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.utils.AttributeSchemaUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.Map;

public class PrimaryEntityFilterBuilder extends FilterBuilder {
  public PrimaryEntityFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  protected EntityFilter buildForCohort(Underlay underlay, SelectionData selectionData) {
    CFPlaceholder.Placeholder config = deserializeConfig();
    DTAttribute.Attribute data = deserializeData(selectionData.getPluginData());
    return AttributeSchemaUtils.buildForEntity(underlay, underlay.getPrimaryEntity(), config, data);
  }

  @Override
  protected Map<Entity, EntityFilter> buildForDataFeature(
      Underlay underlay, SelectionData selectionData) {
    return Map.of(underlay.getPrimaryEntity(), null);
  }

  @Override
  public CFPlaceholder.Placeholder deserializeConfig() {
    return AttributeSchemaUtils.deserializeConfig(criteriaSelector.getPluginConfig());
  }

  @Override
  public DTAttribute.Attribute deserializeData(String serialized) {
    return AttributeSchemaUtils.deserializeData(serialized);
  }
}
