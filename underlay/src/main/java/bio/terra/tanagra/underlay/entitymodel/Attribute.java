package bio.terra.tanagra.underlay.entitymodel;

import bio.terra.tanagra.api.shared.DataType;
import java.util.Objects;

public final class Attribute {
  private final String name;
  private final DataType dataType;
  private final boolean isValueDisplay;
  private final boolean isId;
  private final String runtimeSqlFunctionWrapper;
  private final DataType runtimeDataType;
  private final boolean isComputeDisplayHint;
  private final SourceQuery sourceQuery;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public Attribute(
      String name,
      DataType dataType,
      boolean isValueDisplay,
      boolean isId,
      String runtimeSqlFunctionWrapper,
      DataType runtimeDataType,
      boolean isComputeDisplayHint,
      SourceQuery sourceQuery) {
    this.name = name;
    this.dataType = dataType;
    this.isValueDisplay = isValueDisplay;
    this.isId = isId;
    this.runtimeSqlFunctionWrapper = runtimeSqlFunctionWrapper;
    this.runtimeDataType = runtimeDataType;
    this.isComputeDisplayHint = isComputeDisplayHint && !isId;
    this.sourceQuery = sourceQuery;
  }

  public String getName() {
    return name;
  }

  public DataType getDataType() {
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

  public DataType getRuntimeDataType() {
    return runtimeDataType == null ? dataType : runtimeDataType;
  }

  public boolean isComputeDisplayHint() {
    return isComputeDisplayHint;
  }

  public SourceQuery getSourceQuery() {
    return sourceQuery;
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
        && runtimeDataType == attribute.runtimeDataType
        && Objects.equals(sourceQuery, attribute.sourceQuery);
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
        isComputeDisplayHint,
        sourceQuery);
  }

  public static class SourceQuery {
    private final boolean isSuppressed;

    private final String valueFieldName;

    private final String displayFieldTable;

    private final String displayFieldName;

    private final String displayFieldTableJoinFieldName;

    public SourceQuery(
        boolean isSuppressed,
        String valueFieldName,
        String displayFieldTable,
        String displayFieldName,
        String displayFieldTableJoinFieldName) {
      this.isSuppressed = isSuppressed;
      this.valueFieldName = valueFieldName;
      this.displayFieldTable = displayFieldTable;
      this.displayFieldName = displayFieldName;
      this.displayFieldTableJoinFieldName = displayFieldTableJoinFieldName;
    }

    public boolean isSuppressed() {
      return isSuppressed;
    }

    public String getValueFieldName() {
      return valueFieldName;
    }

    public String getDisplayFieldTable() {
      return displayFieldTable;
    }

    public String getDisplayFieldName() {
      return displayFieldName;
    }

    public boolean hasDisplayField() {
      return displayFieldName != null;
    }

    public String getDisplayFieldTableJoinFieldName() {
      return displayFieldTableJoinFieldName;
    }

    public boolean hasDisplayFieldTableJoin() {
      return displayFieldTable != null && !displayFieldTable.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SourceQuery that = (SourceQuery) o;
      return isSuppressed == that.isSuppressed
          && valueFieldName.equals(that.valueFieldName)
          && Objects.equals(displayFieldTable, that.displayFieldTable)
          && Objects.equals(displayFieldName, that.displayFieldName)
          && Objects.equals(displayFieldTableJoinFieldName, that.displayFieldTableJoinFieldName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          isSuppressed,
          valueFieldName,
          displayFieldTable,
          displayFieldName,
          displayFieldTableJoinFieldName);
    }
  }
}
