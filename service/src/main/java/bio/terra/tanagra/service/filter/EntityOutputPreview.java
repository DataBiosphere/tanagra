package bio.terra.tanagra.service.filter;

import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class EntityOutputPreview {
  private EntityOutput entityOutput;
  private List<Pair<FeatureSet, Criteria>> attributedCriteria;
  private List<ValueDisplayField> selectedFields;

  public EntityOutput getEntityOutput() {
    return entityOutput;
  }

  public EntityOutputPreview setEntityOutput(EntityOutput entityOutput) {
    this.entityOutput = entityOutput;
    return this;
  }

  public List<Pair<FeatureSet, Criteria>> getAttributedCriteria() {
    return attributedCriteria;
  }

  public EntityOutputPreview setAttributedCriteria(
      List<Pair<FeatureSet, Criteria>> attributedCriteria) {
    this.attributedCriteria = attributedCriteria;
    return this;
  }

  public List<ValueDisplayField> getSelectedFields() {
    return selectedFields;
  }

  public EntityOutputPreview setSelectedFields(List<ValueDisplayField> selectedFields) {
    this.selectedFields = selectedFields;
    return this;
  }
}
