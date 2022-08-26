package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFAttributeMapping;
import java.util.List;
import javax.annotation.Nullable;

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
