package bio.terra.tanagra.api.field;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.*;

public class CountDistinctField extends ValueDisplayField {
  private final Underlay underlay;
  private final Entity entity;
  private final Attribute attribute;

  public CountDistinctField(Underlay underlay, Entity entity, Attribute attribute) {
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  public Attribute getAttribute() {
    return attribute;
  }

  @Override
  public DataType getDataType() {
    return DataType.INT64;
  }
}
