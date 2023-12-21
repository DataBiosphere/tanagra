package bio.terra.tanagra.api.field;

import bio.terra.tanagra.query.Literal;

public abstract class ValueDisplayField {
  public abstract Literal.DataType getDataType();
}
