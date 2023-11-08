package bio.terra.tanagra.underlay.entitymodel;

import bio.terra.tanagra.query.Literal;
import java.util.Objects;

public final class Attribute {
  private final String name;
  private final Literal.DataType dataType;
  private final boolean isValueDisplay;
  private final boolean isId;
  private final String runtimeSqlFunctionWrapper;
  private final Literal.DataType runtimeDataType;
  private final boolean isComputeDisplayHint;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public Attribute(
      String name,
      Literal.DataType dataType,
      boolean isValueDisplay,
      boolean isId,
      String runtimeSqlFunctionWrapper,
      Literal.DataType runtimeDataType,
      boolean isComputeDisplayHint) {
    this.name = name;
    this.dataType = dataType;
    this.isValueDisplay = isValueDisplay;
    this.isId = isId;
    this.runtimeSqlFunctionWrapper = runtimeSqlFunctionWrapper;
    this.runtimeDataType = runtimeDataType;
    this.isComputeDisplayHint = isComputeDisplayHint && !isId;
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

  public String getRuntimeSqlFunctionWrapper() {
    return runtimeSqlFunctionWrapper;
  }

  public boolean hasRuntimeSqlFunctionWrapper() {
    return runtimeSqlFunctionWrapper != null;
  }

  public Literal.DataType getRuntimeDataType() {
    return runtimeDataType == null ? dataType : runtimeDataType;
  }

  public boolean isComputeDisplayHint() {
    return isComputeDisplayHint;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Attribute attribute = (Attribute) o;
    return isValueDisplay == attribute.isValueDisplay
        && isId == attribute.isId
        && isComputeDisplayHint == attribute.isComputeDisplayHint
        && name.equals(attribute.name)
        && dataType == attribute.dataType
        && Objects.equals(runtimeSqlFunctionWrapper, attribute.runtimeSqlFunctionWrapper)
        && runtimeDataType == attribute.runtimeDataType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        name,
        dataType,
        isValueDisplay,
        isId,
        runtimeSqlFunctionWrapper,
        runtimeDataType,
        isComputeDisplayHint);
  }
}
