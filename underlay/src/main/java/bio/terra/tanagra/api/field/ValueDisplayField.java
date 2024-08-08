package bio.terra.tanagra.api.field;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.underlay.entitymodel.*;

public abstract class ValueDisplayField {
  public abstract Entity getEntity();

  public abstract DataType getDataType();
}
