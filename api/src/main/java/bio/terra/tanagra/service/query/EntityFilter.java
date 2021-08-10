package bio.terra.tanagra.service.query;

import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Filter;
import com.google.auto.value.AutoValue;

/** A query for entities matching a filter. */
@AutoValue
public abstract class EntityFilter {
  /** The primary entity and variable that is being filtered on. */
  public abstract EntityVariable primaryEntity();

  /** The filter to apply to the primary entity. */
  public abstract Filter filter();

  public static Builder builder() {
    return new AutoValue_EntityFilter.Builder();
  }

  /** Builder for {@link EntityFilter}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder primaryEntity(EntityVariable primaryEntity);

    public abstract Builder filter(Filter filter);

    public abstract EntityFilter build();
  }
}
