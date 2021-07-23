package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;

/** A search query on an {@link Entity}. */
@AutoValue
public abstract class Query {
  /** What to select for the query. */
  // TODO consider selections with a different entty than the primary entity.
  public abstract ImmutableList<Selection> selections();

  /** The primary entity being queried. */
  public abstract Entity primaryEntity();

  /** The filter to apply to the primary entity, if there is a filter. */
  public abstract Optional<Filter> filter();

  public static Query create(
      List<Selection> selections, Entity primaryEntity, Optional<Filter> predicate) {
    Preconditions.checkArgument(!selections.isEmpty(), "A Query must have non-zero selections");
    return new AutoValue_Query(ImmutableList.copyOf(selections), primaryEntity, predicate);
  }
}
