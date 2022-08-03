package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFAttribute;
import com.google.common.base.Strings;

public class Attribute {

  /** Enum for the types of attributes supported by Tanagra. */
  public enum Type {
    SIMPLE,
    KEY_AND_DISPLAY
  }

  private String name;
  private Type type;
  private Literal.DataType dataType;

  private Attribute(String name, Type type, Literal.DataType dataType) {
    this.name = name;
    this.type = type;
    this.dataType = dataType;
  }

  public static Attribute fromSerialized(UFAttribute serialized) {
    if (Strings.isNullOrEmpty(serialized.getName())) {
      throw new IllegalArgumentException("Attribute name is undefined");
    }
    // TODO: populate datatype from BQ
    return new Attribute(serialized.getName(), serialized.getType(), serialized.getDataType());
  }

  // getters
  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public Literal.DataType getDataType() {
    return dataType;
  }
}
