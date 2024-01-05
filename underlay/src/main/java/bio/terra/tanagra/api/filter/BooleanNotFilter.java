package bio.terra.tanagra.api.filter;

public class BooleanNotFilter extends EntityFilter {
  private final EntityFilter subFilter;

  public BooleanNotFilter(EntityFilter subFilter) {
    this.subFilter = subFilter;
  }

  public EntityFilter getSubFilter() {
    return subFilter;
  }
}
