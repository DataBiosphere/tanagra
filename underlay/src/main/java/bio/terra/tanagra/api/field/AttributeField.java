package bio.terra.tanagra.api.field;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;

public class AttributeField extends ValueDisplayField {
  private final Underlay underlay;
  private final Entity entity;
  private final Attribute attribute;
  private final boolean excludeDisplay;
  private final boolean isSource;

  public AttributeField(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      boolean excludeDisplay,
      boolean isSource) {
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.excludeDisplay = excludeDisplay;
    this.isSource = isSource;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  @Override
  public DataType getDataType() {
    return attribute.getRuntimeDataType();
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public boolean isExcludeDisplay() {
    return excludeDisplay;
  }

  public boolean isSource() {
    return isSource;
  }
}
