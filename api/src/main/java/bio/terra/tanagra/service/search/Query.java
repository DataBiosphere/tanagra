package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/** A search query on an {@link Entity}. */
@AutoValue
public abstract class Query {
  /** What to select for the query. */
  // TODO consider selections with a different entity than the primary entity.
  public abstract ImmutableList<Selection> selections();

  /** What to group by for the query. */
  @Nullable
  public abstract ImmutableList<Selection> groupBy();

  /** What to order by for the query. */
  @Nullable
  public abstract Selection orderBy();

  /** The direction to order by for the query. */
  @Nullable
  public abstract OrderByDirection orderByDirection();

  /** The primary entity being queried. */
  public abstract EntityVariable primaryEntity();

  /** The filter to apply to the primary entity, if there is a filter. */
  public abstract Optional<Filter> filter();

  /** The maximum number of results to return */
  @Nullable
  public abstract Integer limit();

  public static Builder builder() {
    return new AutoValue_Query.Builder();
  }

  /** Builder for {@link Query}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder selections(List<Selection> selections);

    public abstract Builder groupBy(List<Selection> groupBy);

    public abstract Builder orderBy(Selection orderBy);

    public abstract Builder orderByDirection(OrderByDirection orderByDirection);

    public abstract Builder primaryEntity(EntityVariable primaryEntity);

    public abstract Builder filter(Optional<Filter> filter);

    public abstract Builder filter(Filter filter);

    public abstract Builder limit(Integer limit);

    public abstract Query build();
  }
}
