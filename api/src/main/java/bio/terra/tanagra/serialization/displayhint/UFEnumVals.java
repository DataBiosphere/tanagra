package bio.terra.tanagra.serialization.displayhint;

import bio.terra.tanagra.serialization.UFDisplayHint;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * External representation of an enum display hint.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFEnumVals.Builder.class)
public class UFEnumVals extends UFDisplayHint {
  private final List<UFValueDisplay> valueDisplays;

  public UFEnumVals(EnumVals displayHint) {
    super(displayHint);
    this.valueDisplays =
        displayHint.getValueDisplays().stream()
            .map(vd -> new UFValueDisplay(vd))
            .collect(Collectors.toList());
  }

  private UFEnumVals(Builder builder) {
    super(builder);
    this.valueDisplays = builder.enumVals;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFDisplayHint.Builder {
    private List<UFValueDisplay> enumVals;

    public Builder enumVals(List<UFValueDisplay> enumVals) {
      this.enumVals = enumVals;
      return this;
    }

    /** Call the private constructor. */
    @Override
    public UFEnumVals build() {
      return new UFEnumVals(this);
    }
  }

  /** Deserialize to the internal representation of the display hint. */
  @Override
  public EnumVals deserializeToInternal() {
    return EnumVals.fromSerialized(this);
  }

  public List<UFValueDisplay> getValueDisplays() {
    return valueDisplays;
  }
}
