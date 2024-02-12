package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.filterbuilder.CriteriaSelector;
import java.util.List;
import java.util.Map;

public abstract class FilterBuilder {
  protected final CriteriaSelector criteriaSelector;

  public FilterBuilder(CriteriaSelector criteriaSelector) {
    this.criteriaSelector = criteriaSelector;
  }

  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new UnsupportedOperationException(
          "Filter builder only allows a single selection data, found " + selectionData.size());
    }
    return buildForCohort(underlay, selectionData.get(0));
  }

  protected abstract EntityFilter buildForCohort(Underlay underlay, SelectionData selectionData);

  public Map<Entity, EntityFilter> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new UnsupportedOperationException(
          "Filter builder only allows a single selection data, found " + selectionData.size());
    }
    return buildForDataFeature(underlay, selectionData.get(0));
  }

  protected abstract Map<Entity, EntityFilter> buildForDataFeature(
      Underlay underlay, SelectionData selectionData);

  public abstract <T> T deserializeConfig();

  public abstract <T> T deserializeData(String serialized);
}
