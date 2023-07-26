package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.serialization.UFAttribute;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Attribute {

  /** Enum for the types of attributes supported by Tanagra. */
  public enum Type {
    SIMPLE,
    KEY_AND_DISPLAY
  }

  private final String name;
  private final Type type;
  private Literal.DataType dataType;
  private final List<DisplayHint.Type> displayHintTypes;
  private AttributeMapping sourceMapping;
  private AttributeMapping indexMapping;

  public Attribute(
      String name, Type type, Literal.DataType dataType, List<DisplayHint.Type> displayHintTypes) {
    this.name = name;
    this.type = type;
    this.dataType = dataType;
    this.displayHintTypes =
        displayHintTypes == null ? new ArrayList<>() : new ArrayList<>(displayHintTypes);
  }

  public void initialize(AttributeMapping sourceMapping, AttributeMapping indexMapping) {
    this.sourceMapping = sourceMapping;
    this.indexMapping = indexMapping;
    // sourceMapping is null for age_of_occurrence attribute
    if (sourceMapping != null) {
      sourceMapping.initialize(this);
    }
    indexMapping.initialize(this);
  }

  public static Attribute fromSerialized(UFAttribute serialized) {
    if (Strings.isNullOrEmpty(serialized.getName())) {
      throw new InvalidConfigException("Attribute name is undefined");
    }
    return new Attribute(
        serialized.getName(),
        serialized.getType(),
        serialized.getDataType(),
        serialized.getDisplayHintTypes());
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public Literal.DataType getDataType() {
    return dataType;
  }

  public void setDataType(Literal.DataType dataType) {
    this.dataType = dataType;
  }

  public List<DisplayHint.Type> getDisplayHintTypes() {
    return Collections.unmodifiableList(displayHintTypes);
  }

  public boolean skipCalculateDisplayHint() {
    return List.of(DisplayHint.Type.NONE).equals(displayHintTypes);
  }

  public AttributeMapping getMapping(Underlay.MappingType mappingType) {
    return Underlay.MappingType.SOURCE.equals(mappingType) ? sourceMapping : indexMapping;
  }
}
