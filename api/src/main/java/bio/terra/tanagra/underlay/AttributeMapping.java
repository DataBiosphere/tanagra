package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFAttributeMapping;
import javax.annotation.Nullable;

public class AttributeMapping {
  private static String DEFAULT_DISPLAY_MAPPING_PREFIX = "t_display_";

  private FieldPointer value;
  private FieldPointer display;

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
        (serialized != null && serialized.value != null)
            ? FieldPointer.fromSerialized(serialized.value, tablePointer)
            : new FieldPointer(tablePointer, attribute.getName());

    switch (attribute.getType()) {
      case SIMPLE:
        return new AttributeMapping(value);
      case KEY_AND_DISPLAY:
        // if the display is defined, then deserialize it
        // otherwise, generate a default attribute display mapping: a column with the same name as
        // the attribute with a prefix
        FieldPointer display =
            (serialized != null && serialized.display != null)
                ? FieldPointer.fromSerialized(serialized.display, tablePointer)
                : new FieldPointer(
                    tablePointer, DEFAULT_DISPLAY_MAPPING_PREFIX + attribute.getName());
        return new AttributeMapping(value, display);
      default:
        throw new IllegalArgumentException("Attribute type is not defined");
    }
  }
}
