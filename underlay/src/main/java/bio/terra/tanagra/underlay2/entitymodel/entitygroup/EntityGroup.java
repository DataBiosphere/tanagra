package bio.terra.tanagra.underlay2.entitymodel.entitygroup;

import bio.terra.tanagra.underlay2.entitymodel.Relationship;
import com.google.common.collect.ImmutableSet;

public abstract class EntityGroup {
  public enum Type {
    GROUP_ITEMS,
    CRITERIA_OCCURRENCE
  }

  private final String name;

  protected EntityGroup(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract Type getType();

  public abstract boolean includesEntity(String name);

  public abstract ImmutableSet<Relationship> getRelationships();
}
