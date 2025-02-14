package bio.terra.tanagra.api.query.hint;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import jakarta.annotation.Nullable;

public record HintQueryRequest(
    Underlay underlay,
    Entity hintedEntity,
    @Nullable Entity relatedEntity,
    @Nullable Literal relatedEntityId,
    @Nullable EntityGroup entityGroup,
    boolean isDryRun) {
  public boolean isEntityLevel() {
    return relatedEntity == null || relatedEntityId == null || entityGroup == null;
  }
}
