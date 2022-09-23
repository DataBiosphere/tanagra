package bio.terra.tanagra.indexing;

import bio.terra.tanagra.underlay.Entity;

public abstract class EntityJob implements IndexingJob {
  private final Entity entity;

  protected EntityJob(Entity entity) {
    this.entity = entity;
  }

  @Override
  public boolean prerequisitesComplete() {
    // Entity jobs have no prerequisites.
    return true;
  }

  public Entity getEntity() {
    return entity;
  }
}
