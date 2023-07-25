package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.serialization.displayhint.UFValueDisplay;
import java.util.Objects;

public class ValueDisplay {
  private final Literal value;
  private final String display;

  public ValueDisplay(Literal value) {
    this.value = value;
    this.display = null;
  }

  public ValueDisplay(Literal value, String display) {
    this.value = value;
    this.display = display;
  }

  public ValueDisplay(String valueAndDisplay) {
    this.value = new Literal(valueAndDisplay);
    this.display = valueAndDisplay;
  }

  public static ValueDisplay fromSerialized(UFValueDisplay serialized) {
    if (serialized.getValue() == null) {
      throw new InvalidConfigException("Value is undefined");
    }
    return new ValueDisplay(Literal.fromSerialized(serialized.getValue()), serialized.getDisplay());
  }

  public Literal getValue() {
    return value;
  }

  public String getDisplay() {
    return display;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValueDisplay that = (ValueDisplay) o;
    return value.equals(that.value) && Objects.equals(display, that.display);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, display);
  }
}
