package bio.terra.tanagra.filterbuilder.impl.core;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.SelectionData;
import bio.terra.tanagra.filterbuilder.impl.utils.AttributeSchemaUtils;
import bio.terra.tanagra.filterbuilder.schema.attribute.PSAttributeConfig;
import bio.terra.tanagra.filterbuilder.schema.attribute.PSAttributeData;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.utils.JacksonMapper;
import java.util.Map;

public class PrimaryEntityFilterBuilder extends FilterBuilder {
  public PrimaryEntityFilterBuilder(Underlay underlay, String configSerialized) {
    super(underlay, configSerialized);
  }

  @Override
  protected EntityFilter buildForCohort(SelectionData selectionData) {
    PSAttributeConfig config = deserializeConfig();
    PSAttributeData data = deserializeData(selectionData.getSerialized());
    return AttributeSchemaUtils.buildForEntity(underlay, underlay.getPrimaryEntity(), config, data);
  }

  @Override
  protected Map<Entity, EntityFilter> buildForDataFeature(SelectionData selectionData) {
    return Map.of(underlay.getPrimaryEntity(), null);
  }

  @Override
  public PSAttributeConfig deserializeConfig() {
    return JacksonMapper.deserializeJavaObject(
        configSerialized,
        PSAttributeConfig.class,
        (jpEx) -> new InvalidConfigException("Error deserializing criteria selector config", jpEx));
  }

  @Override
  public PSAttributeData deserializeData(String serialized) {
    return AttributeSchemaUtils.deserializeData(serialized);
  }
}
