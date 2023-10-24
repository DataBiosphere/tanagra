package bio.terra.tanagra.underlay2.entitygroup;

import bio.terra.tanagra.query.FieldPointer;
import javax.annotation.Nullable;

public abstract class EntityGroup {
  public enum Type {
    GROUP_ITEMS,
    CRITERIA_OCCURRENCE
  }

  private final String name;

  private final String description;

  protected EntityGroup(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public abstract Type getType();

  public abstract boolean includesEntity(String name);

  public abstract FieldPointer getCountField(
      String countForEntity, String countedEntity, @Nullable String countForHierarchy);
}
