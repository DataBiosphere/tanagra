package bio.terra.tanagra.underlay.displayhint;

import bio.terra.tanagra.serialization.displayhint.UFEnumVals;
import bio.terra.tanagra.underlay.DisplayHint;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class EnumVals extends DisplayHint {
  private final List<ValueDisplay> valueDisplays;

  public EnumVals(List<ValueDisplay> valueDisplays) {
    this.valueDisplays = valueDisplays;
  }

  public static EnumVals fromSerialized(UFEnumVals serialized) {
    if (serialized.getValueDisplays() == null) {
      throw new IllegalArgumentException("Enum values map is undefined");
    }
    List<ValueDisplay> valueDisplays =
        serialized.getValueDisplays().stream()
            .map(vd -> ValueDisplay.fromSerialized(vd))
            .collect(Collectors.toList());
    return new EnumVals(valueDisplays);
  }

  @Override
  public Type getType() {
    return Type.ENUM;
  }

  public List<ValueDisplay> getValueDisplays() {
    return Collections.unmodifiableList(valueDisplays);
  }
}
