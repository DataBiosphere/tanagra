package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;

public class GroupHasItemsFilter extends EntityFilter {
  private final Underlay underlay;
  private final GroupItems groupItems;

  public GroupHasItemsFilter(Underlay underlay, GroupItems groupItems) {
    this.underlay = underlay;
    this.groupItems = groupItems;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public GroupItems getGroupItems() {
    return groupItems;
  }
}
