package bio.terra.tanagra.api.query;

import bio.terra.tanagra.query.Literal;
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
