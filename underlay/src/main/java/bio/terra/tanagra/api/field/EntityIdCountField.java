package bio.terra.tanagra.api.field;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;

public class EntityIdCountField extends ValueDisplayField {
  private final Underlay underlay;
  private final Entity entity;

  public EntityIdCountField(Underlay underlay, Entity entity) {
    this.underlay = underlay;
    this.entity = entity;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  @Override
  public Literal.DataType getDataType() {
    return Literal.DataType.INT64;
  }
}
