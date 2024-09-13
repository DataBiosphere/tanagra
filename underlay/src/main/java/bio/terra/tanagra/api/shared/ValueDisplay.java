package bio.terra.tanagra.api.shared;

import com.google.common.collect.*;
import java.util.*;

public class ValueDisplay {
  private final Literal value;
  private final String display;
  private final ImmutableList<Literal> repeatedValue;

  public ValueDisplay(Literal value) {
    this.value = value;
    this.display = null;
    this.repeatedValue = null;
  }

  public ValueDisplay(Literal value, String display) {
    this.value = value;
    this.display = display;
    this.repeatedValue = null;
  }

  public ValueDisplay(List<Literal> repeatedValue) {
    this.value = null;
    this.display = null;
    this.repeatedValue = ImmutableList.copyOf(repeatedValue);
  }

  public Literal getValue() {
    return value;
  }

  public String getDisplay() {
    return display;
  }

  public List<Literal> getRepeatedValue() {
    return repeatedValue;
  }

  public boolean isRepeatedValue() {
    return repeatedValue != null;
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
    return Objects.equals(value, that.value)
        && Objects.equals(display, that.display)
        && Objects.equals(repeatedValue, that.repeatedValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, display, repeatedValue);
  }
}
