package bio.terra.tanagra.underlay.entitymodel;

import bio.terra.tanagra.api.shared.DataType;
import java.util.Objects;

public final class Attribute {
  private final String name;
  private final DataType dataType;
  private final boolean isDataTypeRepeated;
  private final boolean isValueDisplay;
  private final boolean isId;
  private final String runtimeSqlFunctionWrapper;
  private final DataType runtimeDataType;
  private final boolean isComputeDisplayHint;
  private final boolean isSuppressedForExport;
  private final boolean isVisitDateForTemporalQuery;
  private final boolean isVisitIdForTemporalQuery;
  private final SourceQuery sourceQuery;

  @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
  public Attribute(
      String name,
      DataType dataType,
      boolean isDataTypeRepeated,
      boolean isValueDisplay,
      boolean isId,
      String runtimeSqlFunctionWrapper,
      DataType runtimeDataType,
      boolean isComputeDisplayHint,
      boolean isSuppressedForExport,
      boolean isVisitDateForTemporalQuery,
      boolean isVisitIdForTemporalQuery,
      SourceQuery sourceQuery) {
    this.name = name;
    this.dataType = dataType;
    this.isDataTypeRepeated = isDataTypeRepeated;
    this.isValueDisplay = isValueDisplay;
    this.isId = isId;
    this.runtimeSqlFunctionWrapper = runtimeSqlFunctionWrapper;
    this.runtimeDataType = runtimeDataType;
    this.isVisitDateForTemporalQuery = isVisitDateForTemporalQuery;
    this.isVisitIdForTemporalQuery = isVisitIdForTemporalQuery;
    this.isComputeDisplayHint = isComputeDisplayHint && !isId;
    this.isSuppressedForExport = isSuppressedForExport;
    this.sourceQuery = sourceQuery;
  }

  public String getName() {
    return name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public boolean isDataTypeRepeated() {
    return isDataTypeRepeated;
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

  public boolean isSuppressedForExport() {
    return isSuppressedForExport;
  }

  public boolean isVisitDateForTemporalQuery() {
    return isVisitDateForTemporalQuery;
  }

  public boolean isVisitIdForTemporalQuery() {
    return isVisitIdForTemporalQuery;
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
        && isSuppressedForExport == attribute.isSuppressedForExport
        && isVisitDateForTemporalQuery == attribute.isVisitDateForTemporalQuery
        && isVisitIdForTemporalQuery == attribute.isVisitIdForTemporalQuery
        && name.equals(attribute.name)
        && dataType == attribute.dataType
        && isDataTypeRepeated == attribute.isDataTypeRepeated
        && Objects.equals(runtimeSqlFunctionWrapper, attribute.runtimeSqlFunctionWrapper)
        && runtimeDataType == attribute.runtimeDataType
        && Objects.equals(sourceQuery, attribute.sourceQuery);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        name,
        dataType,
        isDataTypeRepeated,
        isValueDisplay,
        isId,
        runtimeSqlFunctionWrapper,
        runtimeDataType,
        isComputeDisplayHint,
        isSuppressedForExport,
        isVisitDateForTemporalQuery,
        isVisitIdForTemporalQuery,
        sourceQuery);
  }

  public static class SourceQuery {
    private final String valueFieldName;

    private final String displayFieldTable;

    private final String displayFieldName;

    private final String displayFieldTableJoinFieldName;

    public SourceQuery(
        String valueFieldName,
        String displayFieldTable,
        String displayFieldName,
        String displayFieldTableJoinFieldName) {
      this.valueFieldName = valueFieldName;
      this.displayFieldTable = displayFieldTable;
      this.displayFieldName = displayFieldName;
      this.displayFieldTableJoinFieldName = displayFieldTableJoinFieldName;
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
      return valueFieldName.equals(that.valueFieldName)
          && Objects.equals(displayFieldTable, that.displayFieldTable)
          && Objects.equals(displayFieldName, that.displayFieldName)
          && Objects.equals(displayFieldTableJoinFieldName, that.displayFieldTableJoinFieldName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          valueFieldName, displayFieldTable, displayFieldName, displayFieldTableJoinFieldName);
    }
  }
}
