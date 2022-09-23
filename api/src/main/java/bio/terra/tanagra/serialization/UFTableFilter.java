package bio.terra.tanagra.serialization;

import bio.terra.tanagra.serialization.tablefilter.UFArrayFilter;
import bio.terra.tanagra.serialization.tablefilter.UFBinaryFilter;
import bio.terra.tanagra.underlay.TableFilter;
import bio.terra.tanagra.underlay.TablePointer;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a table filter.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = UFBinaryFilter.class, name = "BINARY"),
  @JsonSubTypes.Type(value = UFArrayFilter.class, name = "ARRAY")
})
@JsonDeserialize(builder = UFTableFilter.Builder.class)
public abstract class UFTableFilter {
  private final TableFilter.Type type;

  public UFTableFilter(TableFilter tableFilter) {
    this.type = tableFilter.getType();
  }

  protected UFTableFilter(Builder builder) {
    this.type = builder.type;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public abstract static class Builder {
    private TableFilter.Type type;

    public Builder type(TableFilter.Type type) {
      this.type = type;
      return this;
    }

    /** Call the private constructor. */
    public abstract UFTableFilter build();
  }

  /** Deserialize to the internal representation of the table filter. */
  public abstract TableFilter deserializeToInternal(TablePointer tablePointer);

  public TableFilter.Type getType() {
    return type;
  }
}
