package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.proto.criteriaselector.KeyOuterClass;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;

public abstract class FilterBuilder<CF, DT> {
  protected final CriteriaSelector criteriaSelector;

  public FilterBuilder(CriteriaSelector criteriaSelector) {
    this.criteriaSelector = criteriaSelector;
  }

  public abstract EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData);

  public abstract List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData);

  public abstract CF deserializeConfig();

  public abstract DT deserializeData(String serialized);

  protected static Literal keyToLiteral(KeyOuterClass.Key key) {
    if (key.hasStringKey()) {
      return Literal.forString(key.getStringKey());
    } else if (key.hasInt64Key()) {
      return Literal.forInt64(key.getInt64Key());
    }
    throw new InvalidQueryException("Unsupported key type");
  }
}
