package bio.terra.tanagra.api;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import java.util.List;

public abstract class EntityFilter {
  private final Entity entity;
  private final EntityMapping entityMapping;

  public EntityFilter(Entity entity, EntityMapping entityMapping) {
    this.entity = entity;
    this.entityMapping = entityMapping;
  }

  public abstract FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars);

  protected Entity getEntity() {
    return entity;
  }

  protected EntityMapping getEntityMapping() {
    return entityMapping;
  }
}
