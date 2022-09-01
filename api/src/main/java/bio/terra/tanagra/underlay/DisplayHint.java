package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFDisplayHint;
import bio.terra.tanagra.serialization.displayhint.UFEnumVals;
import bio.terra.tanagra.serialization.displayhint.UFNumericRange;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;

public abstract class DisplayHint {
  public enum Type {
    ENUM,
    RANGE
  }

  public abstract Type getType();

  public UFDisplayHint serialize() {
    if (getType().equals(Type.ENUM)) {
      return new UFEnumVals((EnumVals) this);
    } else if (getType().equals(Type.RANGE)) {
      return new UFNumericRange((NumericRange) this);
    } else {
      throw new IllegalArgumentException("Unknown display hint type: " + getType());
    }
  }
}
