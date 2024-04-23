package bio.terra.tanagra.service.filter;

import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Criteria;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class EntityOutputAndAttributedCriteria {
  private final EntityOutput entityOutput;
  private final List<Pair<ConceptSet, Criteria>> attributedCriteria;

  public EntityOutputAndAttributedCriteria(
      EntityOutput entityOutput, List<Pair<ConceptSet, Criteria>> attributedCriteria) {
    this.entityOutput = entityOutput;
    this.attributedCriteria = attributedCriteria;
  }

  public EntityOutput getEntityOutput() {
    return entityOutput;
  }

  public List<Pair<ConceptSet, Criteria>> getAttributedCriteria() {
    return attributedCriteria;
  }
}
