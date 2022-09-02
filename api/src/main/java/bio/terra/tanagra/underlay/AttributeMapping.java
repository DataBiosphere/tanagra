package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFAttributeMapping;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AttributeMapping {
  private static final String DEFAULT_DISPLAY_MAPPING_PREFIX = "t_display_";

  private final FieldPointer value;
  private final FieldPointer display;

  private AttributeMapping(FieldPointer value) {
    this.value = value;
    this.display = null;
  }

  private AttributeMapping(FieldPointer value, FieldPointer display) {
    this.value = value;
    this.display = display;
  }

  public static AttributeMapping fromSerialized(
      @Nullable UFAttributeMapping serialized, TablePointer tablePointer, Attribute attribute) {
    // if the value is defined, then deserialize it
    // otherwise generate a default attribute mapping: a column with the same name as the attribute
    FieldPointer value =
        (serialized != null && serialized.getValue() != null)
            ? FieldPointer.fromSerialized(serialized.getValue(), tablePointer)
            : new FieldPointer(tablePointer, attribute.getName());

    switch (attribute.getType()) {
      case SIMPLE:
        return new AttributeMapping(value);
      case KEY_AND_DISPLAY:
        // if the display is defined, then deserialize it
        // otherwise, generate a default attribute display mapping: a column with the same name as
        // the attribute with a prefix
        FieldPointer display =
            (serialized != null && serialized.getDisplay() != null)
                ? FieldPointer.fromSerialized(serialized.getDisplay(), tablePointer)
                : new FieldPointer(
                    tablePointer, DEFAULT_DISPLAY_MAPPING_PREFIX + attribute.getName());
        return new AttributeMapping(value, display);
      default:
        throw new IllegalArgumentException("Attribute type is not defined");
    }
  }

  public List<FieldVariable> buildFieldVariables(
      TableVariable primaryTable, List<TableVariable> tableVariables, String attributeName) {
    FieldVariable valueVariable = value.buildVariable(primaryTable, tableVariables, attributeName);
    if (!hasDisplay()) {
      return List.of(valueVariable);
    }

    FieldVariable displayVariable =
        display.buildVariable(
            primaryTable, tableVariables, DEFAULT_DISPLAY_MAPPING_PREFIX + attributeName);
    return List.of(valueVariable, displayVariable);
  }

  public Literal.DataType computeDataType() {
    DataPointer dataPointer = value.getTablePointer().getDataPointer();
    return dataPointer.lookupDatatype(value);
  }

  public DisplayHint computeDisplayHint(Attribute attribute) {
    // skip key_and_display attributes for now
    if (attribute.getType().equals(Attribute.Type.KEY_AND_DISPLAY)) {
      return null;
    }

    // skip attributes that have sql function wrappers for now
    if (value.hasSqlFunctionWrapper()) {
      return null;
    }

    switch (attribute.getDataType()) {
      case BOOLEAN:
        return null; // boolean values are enum by default
      case INT64:
        return NumericRange.computeForField(value);
      case STRING:
        return EnumVals.computeForField(value);
      default:
        throw new IllegalArgumentException(
            "Unknown attribute data type: " + attribute.getDataType());
    }
  }

  public boolean hasDisplay() {
    return display != null;
  }

  public FieldPointer getValue() {
    return value;
  }

  public FieldPointer getDisplay() {
    return display;
  }
}
