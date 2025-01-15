package bio.terra.tanagra.underlay.entitymodel.entitygroup;

import bio.terra.tanagra.underlay.entitymodel.Relationship;
import com.google.common.collect.ImmutableSet;

public abstract class EntityGroup {
  public enum Type {
    GROUP_ITEMS,
    CRITERIA_OCCURRENCE
  }

  private final String name;
  private final boolean useSourceIdPairsSql;

  protected EntityGroup(String name, boolean useSourceIdPairsSql) {
    this.name = name;
    this.useSourceIdPairsSql = useSourceIdPairsSql;
  }

  public String getName() {
    return name;
  }

  public boolean isUseSourceIdPairsSql() {
    return useSourceIdPairsSql;
  }

  public abstract Type getType();

  public abstract boolean includesEntity(String name);

  public abstract ImmutableSet<Relationship> getRelationships();

  public abstract boolean hasRollupCountField(String entity, String countedEntity);
}
