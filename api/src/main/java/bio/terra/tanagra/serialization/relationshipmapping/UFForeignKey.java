package bio.terra.tanagra.serialization.relationshipmapping;

import bio.terra.tanagra.serialization.UFRelationshipMapping;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a pointer to a column via foreign keyß.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFForeignKey.Builder.class)
public class UFForeignKey extends UFRelationshipMapping {
  private final String column;
  private final String foreignTable;
  private final String foreignKey;

  /** Constructor for Jackson deserialization during testing. */
  protected UFForeignKey(Builder builder) {
    super(builder);
    this.column = builder.column;
    this.foreignTable = builder.foreignTable;
    this.foreignKey = builder.foreignKey;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFRelationshipMapping.Builder {
    private String column;
    private String foreignTable;
    private String foreignKey;

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

    /** Call the private constructor. */
    public UFForeignKey build() {
      return new UFForeignKey(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  public String getColumn() {
    return column;
  }

  public String getForeignTable() {
    return foreignTable;
  }

  public String getForeignKey() {
    return foreignKey;
  }
}
