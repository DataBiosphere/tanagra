package bio.terra.tanagra.serialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a pointer to a column in the underlying data.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFFieldPointer.Builder.class)
public class UFFieldPointer {
  public final String column;
  public final String foreignTable;
  public final String foreignKey;
  public final String foreignColumn;
  public final String sqlFunctionWrapper;

  /** Constructor for Jackson deserialization during testing. */
  protected UFFieldPointer(Builder builder) {
    this.column = builder.column;
    this.foreignTable = builder.foreignTable;
    this.foreignKey = builder.foreignKey;
    this.foreignColumn = builder.foreignColumn;
    this.sqlFunctionWrapper = builder.sqlFunctionWrapper;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String column;
    private String foreignTable;
    private String foreignKey;
    private String foreignColumn;
    private String sqlFunctionWrapper;

    public Builder column(String column) {
      this.column = column;
      return this;
    }

    public Builder foreignTable(String foreignTable) {
      this.foreignTable = foreignTable;
      return this;
    }

    public Builder foreignKey(String foreignKey) {
      this.foreignKey = foreignKey;
      return this;
    }

    public Builder foreignColumn(String foreignColumn) {
      this.foreignColumn = foreignColumn;
      return this;
    }

    public Builder sqlFunctionWrapper(String sqlFunctionWrapper) {
      this.sqlFunctionWrapper = sqlFunctionWrapper;
      return this;
    }

    /** Call the private constructor. */
    public UFFieldPointer build() {
      return new UFFieldPointer(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
