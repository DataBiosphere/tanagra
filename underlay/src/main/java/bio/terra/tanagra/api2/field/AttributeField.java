package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.Attribute;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttributeField extends EntityField {
  private final Attribute attribute;
  private final boolean excludeDisplay;
  private final boolean isSource;

  public AttributeField(
      Entity entity, Attribute attribute, boolean excludeDisplay, boolean isSource) {
    super(entity);
    this.attribute = attribute;
    this.excludeDisplay = excludeDisplay;
    this.isSource = isSource;
  }

  @Override
  public List<FieldVariable> buildFieldVariables(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    List<FieldVariable> fieldVars = new ArrayList<>();
    fieldVars.add(new FieldVariable(getValueField(), entityTableVar, getValueFieldAlias()));
    if (attribute.isValueDisplay() && !excludeDisplay) {
      fieldVars.add(new FieldVariable(getDisplayField(), entityTableVar, getDisplayFieldAlias()));
    }
    return fieldVars;
  }

  @Override
  public List<ColumnSchema> getColumnSchemas() {
    return (attribute.isSimple() || excludeDisplay)
        ? ImmutableList.of(getValueColumnSchema())
        : ImmutableList.of(getValueColumnSchema(), getDisplayColumnSchema());
  }

  @Override
  public ValueDisplay parseFromRowResult(RowResult rowResult) {
    CellValue cellValue = rowResult.get(getValueFieldAlias());
    if (cellValue == null) {
      throw new SystemException("Attribute value column not found: " + attribute.getName());
    }
    Optional<Literal> valueOpt = cellValue.getLiteral();

    if (valueOpt.isEmpty()) {
      return null;
    } else if (attribute.isSimple()) {
      return new ValueDisplay(valueOpt.get());
    } else { // attribute.isValueDisplay()
      String display =
          rowResult
              .get(getDisplayFieldAlias())
              .getString()
              .orElse(null); // Preserve NULL display values.
      return new ValueDisplay(valueOpt.get(), display);
    }
  }

  private FieldPointer getValueField() {
    return isSource ? attribute.getSourceValueField() : attribute.getIndexValueField();
  }

  private FieldPointer getDisplayField() {
    return isSource ? attribute.getSourceDisplayField() : attribute.getIndexDisplayField();
  }

  private String getValueFieldAlias() {
    return EntityMain.getAttributeValueFieldName(getEntity().getName(), attribute.getName());
  }

  private String getDisplayFieldAlias() {
    return EntityMain.getAttributeDisplayFieldName(getEntity().getName(), attribute.getName());
  }

  private ColumnSchema getValueColumnSchema() {
    return new ColumnSchema(
        getValueFieldAlias(), CellValue.SQLDataType.fromUnderlayDataType(attribute.getDataType()));
  }

  private ColumnSchema getDisplayColumnSchema() {
    return new ColumnSchema(getDisplayFieldAlias(), CellValue.SQLDataType.STRING);
  }
}
