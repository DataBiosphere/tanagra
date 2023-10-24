package bio.terra.tanagra.underlay2;

import static bio.terra.tanagra.underlay2.indexschema.EntityMain.ATTRIBUTE_DISPLAY_SQL_TYPE;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;
import javax.annotation.Nullable;

public final class Attribute {
  private final String name;
  private final Literal.DataType dataType;
  private final boolean isValueDisplay;
  private final boolean isId;
  private final FieldPointer sourceValueField;
  private @Nullable final FieldPointer sourceDisplayField;
  private final FieldPointer indexValueField;
  private @Nullable final FieldPointer indexDisplayField;
  private final boolean isComputeDisplayHint;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public Attribute(
      String entityName,
      String name,
      Literal.DataType dataType,
      boolean isValueDisplay,
      boolean isId,
      FieldPointer sourceValueField,
      FieldPointer sourceDisplayField,
      boolean isComputeDisplayHint) {
    this.name = name;
    this.dataType = dataType;
    this.isValueDisplay = isValueDisplay;
    this.isId = isId;
    this.sourceValueField = sourceValueField;
    this.sourceDisplayField = sourceDisplayField;
    this.isComputeDisplayHint = isComputeDisplayHint;

    // Resolve index fields.
    this.indexValueField = EntityMain.getAttributeValueField(entityName, name);
    this.indexDisplayField =
        !isValueDisplay ? null : EntityMain.getAttributeDisplayField(entityName, name);
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

  public FieldPointer getSourceValueField() {
    return sourceValueField;
  }

  public ColumnSchema getSourceValueColumnSchema() {
    return new ColumnSchema(
        getSourceValueField(), CellValue.SQLDataType.fromUnderlayDataType(dataType));
  }

  public FieldPointer getSourceDisplayField() {
    if (isSimple()) {
      throw new SystemException("No display defined for attribute: " + name);
    }
    return sourceDisplayField;
  }

  public ColumnSchema getSourceDisplayColumnSchema() {
    return new ColumnSchema(getSourceDisplayField(), CellValue.SQLDataType.STRING);
  }

  public FieldPointer getIndexValueField() {
    return indexValueField;
  }

  public ColumnSchema getIndexValueColumnSchema() {
    return new ColumnSchema(
        getIndexValueField(), CellValue.SQLDataType.fromUnderlayDataType(dataType));
  }

  public FieldPointer getIndexDisplayField() {
    if (isSimple()) {
      throw new SystemException("No display defined for attribute: " + name);
    }
    return indexDisplayField;
  }

  public ColumnSchema getIndexDisplayColumnSchema() {
    return new ColumnSchema(getIndexDisplayField(), ATTRIBUTE_DISPLAY_SQL_TYPE);
  }

  public boolean isComputeDisplayHint() {
    return isComputeDisplayHint;
  }
}
