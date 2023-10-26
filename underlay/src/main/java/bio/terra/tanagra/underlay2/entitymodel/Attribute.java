package bio.terra.tanagra.underlay2.entitymodel;

import bio.terra.tanagra.query.Literal;

public final class Attribute {
  private final String name;
  private final Literal.DataType dataType;
  private final boolean isValueDisplay;
  private final boolean isId;
  private final boolean isComputeDisplayHint;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public Attribute(
      String name,
      Literal.DataType dataType,
      boolean isValueDisplay,
      boolean isId,
      boolean isComputeDisplayHint) {
    this.name = name;
    this.dataType = dataType;
    this.isValueDisplay = isValueDisplay;
    this.isId = isId;
    this.isComputeDisplayHint = isComputeDisplayHint;
  }

  public String getName() {
    return name;
  }

  public Literal.DataType getDataType() {
    return dataType;
  }

  public boolean isSimple() {
    return !isValueDisplay;
  }

  public boolean isValueDisplay() {
    return isValueDisplay;
  }

  public boolean isId() {
    return isId;
  }

  public boolean isComputeDisplayHint() {
    return isComputeDisplayHint;
  }
}
