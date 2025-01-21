package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.List;
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class BooleanNotFilter extends EntityFilter {
  private final EntityFilter subFilter;

  public BooleanNotFilter(EntityFilter subFilter) {
    super(
        LoggerFactory.getLogger(BooleanNotFilter.class),
        subFilter.getUnderlay(),
        subFilter.getEntity());
    this.subFilter = subFilter;
  }

  public EntityFilter getSubFilter() {
    return subFilter;
  }

  @Override
  public List<Attribute> getFilterAttributes() {
    return subFilter.getFilterAttributes();
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    BooleanNotFilter that = (BooleanNotFilter) o;
    return subFilter.equals(that.subFilter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), subFilter);
  }
}
