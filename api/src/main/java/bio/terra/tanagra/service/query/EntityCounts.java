package bio.terra.tanagra.service.query;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Filter;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A query for counts of entity instances. Contains the number of entity instances in each group,
 * and a list of the attribute values that define each group.
 * <p>e.g. {20, [Male, American]}, {35, [Female, Japanese]}
 */
@AutoValue
public abstract class EntityCounts {
  /** The primary entity and variable that the counts are being created for. */
  public abstract EntityVariable primaryEntity();

  /**
   * Any additional attributes to return as columns of the dataset.
   * These must be attributes of the primary entity.
   */
  public abstract ImmutableList<Attribute> additionalSelectedAttributes();

  /** The attributes to group by. These must be attributes of the primary entity. */
  public abstract ImmutableList<Attribute> groupByAttributes();

  /** The filter to apply to the primary entity. */
  public abstract Filter filter();

  public static Builder builder() {
    return new AutoValue_EntityCounts.Builder();
  }

  /** Builder for {@link EntityCounts}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder primaryEntity(EntityVariable primaryEntity);

    public abstract EntityVariable primaryEntity();

    public abstract Builder additionalSelectedAttributes(List<Attribute> groupByAttributes);

    public abstract ImmutableList<Attribute> additionalSelectedAttributes();

    public abstract Builder groupByAttributes(List<Attribute> groupByAttributes);

    public abstract ImmutableList<Attribute> groupByAttributes();

    public abstract Builder filter(Filter filter);

    public EntityCounts build() {
      for (Attribute attribute : groupByAttributes()) {
        Preconditions.checkArgument(
            attribute.entity().equals(primaryEntity().entity()),
            "Additional selected attribute's '%s' entity '%s' did not match primary entity '%s'.",
            attribute.name(),
            attribute.entity().name(),
            primaryEntity().entity().name());
      }
      for (Attribute attribute : groupByAttributes()) {
        Preconditions.checkArgument(
            attribute.entity().equals(primaryEntity().entity()),
            "Group by attribute's '%s' entity '%s' did not match primary entity '%s'.",
            attribute.name(),
            attribute.entity().name(),
            primaryEntity().entity().name());
      }
      return autoBuild();
    }

    abstract EntityCounts autoBuild();
  }
}
