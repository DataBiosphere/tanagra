package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.List;
import java.util.Map;

public abstract class FilterBuilder {
  protected final Underlay underlay;
  protected final String configSerialized;

  public FilterBuilder(Underlay underlay, String configSerialized) {
    this.underlay = underlay;
    this.configSerialized = configSerialized;
  }

  public EntityFilter buildForCohort(List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new UnsupportedOperationException(
          "Filter builder only allows a single selection data, found " + selectionData.size());
    }
    return buildForCohort(selectionData.get(0));
  }

  protected abstract EntityFilter buildForCohort(SelectionData selectionData);

  public Map<Entity, EntityFilter> buildForDataFeature(List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new UnsupportedOperationException(
          "Filter builder only allows a single selection data, found " + selectionData.size());
    }
    return buildForDataFeature(selectionData.get(0));
  }

  protected abstract Map<Entity, EntityFilter> buildForDataFeature(SelectionData selectionData);

  public abstract <T> T deserializeConfig();

  public abstract <T> T deserializeData(String serialized);
}
