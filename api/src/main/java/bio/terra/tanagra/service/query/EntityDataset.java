package bio.terra.tanagra.service.query;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.OrderByDirection;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A query for a dataset of entity instances. Contains the selected attributes of an entity with a
 * filter applied on it.
 */
@AutoValue
public abstract class EntityDataset {
  /** The primary entity and variable that the dataset is being created from. */
  public abstract EntityVariable primaryEntity();

  /**
   * The attributes selected to make the columns of the dataset. These must be attributes of the
   * primary entity.
   */
  public abstract ImmutableList<Attribute> selectedAttributes();

  /**
   * The attribute by which to order the entity instances. This must be one of the {@link
   * #selectedAttributes}.
   */
  @Nullable
  public abstract Attribute orderByAttribute();

  /** The direction in which to order the entity instances. */
  @Nullable
  public abstract OrderByDirection orderByDirection();

  /** The filter to apply to the primary entity. */
  public abstract Filter filter();

  public static Builder builder() {
    return new AutoValue_EntityDataset.Builder();
  }

  /** Builder for {@link EntityDataset}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder primaryEntity(EntityVariable primaryEntity);

    public abstract EntityVariable primaryEntity();

    public abstract Builder selectedAttributes(List<Attribute> selectedAttributes);

    public abstract ImmutableList<Attribute> selectedAttributes();

    public abstract Builder orderByAttribute(Attribute orderByAttribute);

    public abstract Attribute orderByAttribute();

    public abstract Builder orderByDirection(OrderByDirection orderByDirection);

    public abstract OrderByDirection orderByDirection();

    public abstract Builder filter(Filter filter);

    public EntityDataset build() {
      for (Attribute attribute : selectedAttributes()) {
        Preconditions.checkArgument(
            attribute.entity().equals(primaryEntity().entity()),
            "Selected attribute's '%s' entity '%s' did not match primary entity '%s'.",
            attribute.name(),
            attribute.entity().name(),
            primaryEntity().entity().name());
      }
      if (orderByAttribute() != null) {
        Preconditions.checkArgument(
            selectedAttributes().contains(orderByAttribute()),
            "Order by attribute '%s' is not one of the selected attributes.",
            orderByAttribute().name());

        // default the order by direction to ASC. only set this if there is an order by attribute
        // specified
        if (orderByDirection() == null) {
          orderByDirection(OrderByDirection.ASC);
        }
      }
      return autoBuild();
    }

    abstract EntityDataset autoBuild();
  }
}
