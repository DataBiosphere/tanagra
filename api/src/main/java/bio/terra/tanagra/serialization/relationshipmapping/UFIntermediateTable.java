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
public class UFIntermediateTable extends UFRelationshipMapping {
  public final String intermediateTable;
  public final String entity1Column;
  public final String entity1ForeignKey;
  public final String entityMColumn;
  public final String entityMForeignKey;

  /** Constructor for Jackson deserialization during testing. */
  protected UFIntermediateTable(Builder builder) {
    super(builder);
    this.intermediateTable = builder.intermediateTable;
    this.entity1Column = builder.entity1Column;
    this.entity1ForeignKey = builder.entity1ForeignKey;
    this.entityMColumn = builder.entityMColumn;
    this.entityMForeignKey = builder.entityMForeignKey;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFRelationshipMapping.Builder {
    private String intermediateTable;
    private String entity1Column;
    private String entity1ForeignKey;
    private String entityMColumn;
    private String entityMForeignKey;

    public Builder intermediateTable(String intermediateTable) {
      this.intermediateTable = intermediateTable;
      return this;
    }

    public Builder entity1Column(String entity1Column) {
      this.entity1Column = entity1Column;
      return this;
    }

    public Builder entity1ForeignKey(String entity1ForeignKey) {
      this.entity1ForeignKey = entity1ForeignKey;
      return this;
    }

    public Builder entityMColumn(String entityMColumn) {
      this.entityMColumn = entityMColumn;
      return this;
    }

    public Builder entityMForeignKey(String entityMForeignKey) {
      this.entityMForeignKey = entityMForeignKey;
      return this;
    }

    /** Call the private constructor. */
    public UFIntermediateTable build() {
      return new UFIntermediateTable(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
