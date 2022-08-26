package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.TablePointer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a pointer to a table in the underlying data.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFTablePointer.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UFTablePointer {
  private final String table;
  private final UFTableFilter filter;

  public UFTablePointer(TablePointer tablePointer) {
    this.table = tablePointer.getTableName();
    this.filter = tablePointer.hasTableFilter() ? tablePointer.getTableFilter().serialize() : null;
  }

  protected UFTablePointer(Builder builder) {
    this.table = builder.table;
    this.filter = builder.filter;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String table;
    private UFTableFilter filter;

    public Builder table(String table) {
      this.table = table;
      return this;
    }

    public Builder filter(UFTableFilter filter) {
      this.filter = filter;
      return this;
    }

    /** Call the private constructor. */
    public UFTablePointer build() {
      return new UFTablePointer(this);
    }
  }

  public String getTable() {
    return table;
  }

  public UFTableFilter getFilter() {
    return filter;
  }
}
