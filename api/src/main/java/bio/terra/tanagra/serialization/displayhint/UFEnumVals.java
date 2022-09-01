package bio.terra.tanagra.serialization.displayhint;

import bio.terra.tanagra.serialization.UFDisplayHint;
import bio.terra.tanagra.serialization.UFLiteral;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * External representation of an enum display hint.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFEnumVals.Builder.class)
public class UFEnumVals extends UFDisplayHint {
  private final Map<UFLiteral, String> enumVals;

  public UFEnumVals(EnumVals displayHint) {
    super(displayHint);
    Map<UFLiteral, String> enumValsMap = new HashMap<>();
    displayHint.getEnumValToDisplayName().entrySet().stream()
        .forEach(ev -> enumValsMap.put(new UFLiteral(ev.getKey()), ev.getValue()));
    this.enumVals = enumValsMap;
  }

  private UFEnumVals(Builder builder) {
    super(builder);
    this.enumVals = builder.enumVals;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFDisplayHint.Builder {
    private Map<UFLiteral, String> enumVals;

    public Builder enumVals(Map<UFLiteral, String> enumVals) {
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

  public Map<UFLiteral, String> getEnumVals() {
    return enumVals;
  }
}
