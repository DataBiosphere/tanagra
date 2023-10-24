package bio.terra.tanagra.api2.query;

import bio.terra.tanagra.query.Literal;

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
}
