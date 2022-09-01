package bio.terra.tanagra.underlay.displayhint;

import bio.terra.tanagra.serialization.displayhint.UFEnumVals;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.Literal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EnumVals extends DisplayHint {
  private final Map<Literal, String> enumValToDisplayName; // value -> display name

  public EnumVals(Map<Literal, String> enumValToDisplayName) {
    this.enumValToDisplayName = enumValToDisplayName;
  }

  public static EnumVals fromSerialized(UFEnumVals serialized) {
    if (serialized.getEnumVals() == null) {
      throw new IllegalArgumentException("Enum values map is undefined");
    }
    Map<Literal, String> enumVals = new HashMap<>();
    serialized.getEnumVals().entrySet().stream()
        .forEach(ev -> enumVals.put(Literal.fromSerialized(ev.getKey()), ev.getValue()));
    return new EnumVals(enumVals);
  }

  @Override
  public Type getType() {
    return Type.ENUM;
  }

  public Map<Literal, String> getEnumValToDisplayName() {
    return Collections.unmodifiableMap(enumValToDisplayName);
  }
}
