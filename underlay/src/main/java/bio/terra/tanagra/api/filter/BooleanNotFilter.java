package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.entitymodel.*;
import java.util.Objects;

public class BooleanNotFilter extends EntityFilter {
  private final EntityFilter subFilter;

  public BooleanNotFilter(EntityFilter subFilter) {
    this.subFilter = subFilter;
  }

  public EntityFilter getSubFilter() {
    return subFilter;
  }

  @Override
  public Entity getEntity() {
    return subFilter.getEntity();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BooleanNotFilter that = (BooleanNotFilter) o;
    return subFilter.equals(that.subFilter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subFilter);
  }
}
