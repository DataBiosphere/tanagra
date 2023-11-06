package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Attribute;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import bio.terra.tanagra.underlay2.sourcetable.STEntityAttributes;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttributeField extends ValueDisplayField {
  private final STEntityAttributes sourceTable;
  private final ITEntityMain indexTable;
  private final Attribute attribute;
  private final boolean excludeDisplay;
  private final boolean isSource;

  public AttributeField(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      boolean excludeDisplay,
      boolean isSource) {
    this.sourceTable = underlay.getSourceSchema().getEntityAttributes(entity.getName());
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
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
    } else { // isValueDisplay
      String display =
          rowResult
              .get(getDisplayFieldAlias())
              .getString()
              .orElse(null); // Preserve NULL display values.
      return new ValueDisplay(valueOpt.get(), display);
    }
  }

  private FieldPointer getValueField() {
    FieldPointer valueField =
        isSource
            ? sourceTable.getValueField(attribute.getName())
            : indexTable.getAttributeValueField(attribute.getName());
    if (attribute.hasRuntimeSqlFunctionWrapper()) {
      return valueField
          .toBuilder()
          .runtimeCalculated(true)
          .sqlFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper())
          .build();
    } else {
      return valueField;
    }
  }

  private FieldPointer getDisplayField() {
    return isSource
        ? sourceTable.getDisplayField(attribute.getName())
        : indexTable.getAttributeDisplayField(attribute.getName());
  }

  private String getValueFieldAlias() {
    return indexTable.getAttributeValueField(attribute.getName()).getColumnName();
  }

  private String getDisplayFieldAlias() {
    return indexTable.getAttributeDisplayField(attribute.getName()).getColumnName();
  }

  private ColumnSchema getValueColumnSchema() {
    return new ColumnSchema(
        getValueFieldAlias(),
        CellValue.SQLDataType.fromUnderlayDataType(attribute.getRuntimeDataType()));
  }

  private ColumnSchema getDisplayColumnSchema() {
    return new ColumnSchema(getDisplayFieldAlias(), CellValue.SQLDataType.STRING);
  }
}
