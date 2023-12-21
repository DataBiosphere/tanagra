package bio.terra.tanagra.api.field.valuedisplay;

import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.sourcetable.STEntityAttributes;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AttributeField extends ValueDisplayField {
  private final STEntityAttributes sourceTable;
  private final ITEntityMain indexTable;
  private final Underlay underlay;
  private final Entity entity;
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
    this.underlay = underlay;
    this.entity = entity;
    this.attribute = attribute;
    this.excludeDisplay = excludeDisplay;
    this.isSource = isSource;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
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
      return new ValueDisplay(new Literal(null));
    } else if (attribute.isSimple() || excludeDisplay) {
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

  public String getValueFieldAlias() {
    return indexTable.getAttributeValueField(attribute.getName()).getColumnName();
  }

  public String getDisplayFieldAlias() {
    return indexTable.getAttributeDisplayField(attribute.getName()).getColumnName();
  }

  public ColumnSchema getValueColumnSchema() {
    return new ColumnSchema(
        getValueFieldAlias(),
        CellValue.SQLDataType.fromUnderlayDataType(attribute.getRuntimeDataType()));
  }

  public ColumnSchema getDisplayColumnSchema() {
    return new ColumnSchema(getDisplayFieldAlias(), CellValue.SQLDataType.STRING);
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public boolean isExcludeDisplay() {
    return excludeDisplay;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttributeField that = (AttributeField) o;
    return excludeDisplay == that.excludeDisplay
        && isSource == that.isSource
        && attribute.equals(that.attribute);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attribute, excludeDisplay, isSource);
  }
}
