package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.RelationshipMapping;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a relationship mapping.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFRelationshipMapping.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UFRelationshipMapping {
  private final UFTablePointer idPairsTable;
  private final UFFieldPointer idPairsIdA;
  private final UFFieldPointer idPairsIdB;

  private final UFTablePointer rollupTableA;
  private final UFFieldPointer rollupIdA;
  private final UFFieldPointer rollupCountA;
  private final UFFieldPointer rollupDisplayHintsA;

  private final UFTablePointer rollupTableB;
  private final UFFieldPointer rollupIdB;
  private final UFFieldPointer rollupCountB;
  private final UFFieldPointer rollupDisplayHintsB;

  public UFRelationshipMapping(RelationshipMapping relationshipMapping) {
    this.idPairsTable = new UFTablePointer(relationshipMapping.getIdPairsTable());
    this.idPairsIdA = new UFFieldPointer(relationshipMapping.getIdPairsIdA());
    this.idPairsIdB = new UFFieldPointer(relationshipMapping.getIdPairsIdB());

    if (relationshipMapping.getRollupTableA() != null) {
      this.rollupTableA = new UFTablePointer(relationshipMapping.getRollupTableA());
      this.rollupIdA = new UFFieldPointer(relationshipMapping.getRollupIdA());
      this.rollupCountA = new UFFieldPointer(relationshipMapping.getRollupCountA());
      this.rollupDisplayHintsA = new UFFieldPointer(relationshipMapping.getRollupDisplayHintsA());
    } else {
      this.rollupTableA = null;
      this.rollupIdA = null;
      this.rollupCountA = null;
      this.rollupDisplayHintsA = null;
    }

    if (relationshipMapping.getRollupTableB() != null) {
      this.rollupTableB = new UFTablePointer(relationshipMapping.getRollupTableB());
      this.rollupIdB = new UFFieldPointer(relationshipMapping.getRollupIdB());
      this.rollupCountB = new UFFieldPointer(relationshipMapping.getRollupCountB());
      this.rollupDisplayHintsB = new UFFieldPointer(relationshipMapping.getRollupDisplayHintsB());
    } else {
      this.rollupTableB = null;
      this.rollupIdB = null;
      this.rollupCountB = null;
      this.rollupDisplayHintsB = null;
    }
  }

  private UFRelationshipMapping(Builder builder) {
    this.idPairsTable = builder.idPairsTable;
    this.idPairsIdA = builder.idPairsIdA;
    this.idPairsIdB = builder.idPairsIdB;

    this.rollupTableA = builder.rollupTableA;
    this.rollupIdA = builder.rollupIdA;
    this.rollupCountA = builder.rollupCountA;
    this.rollupDisplayHintsA = builder.rollupDisplayHintsA;

    this.rollupTableB = builder.rollupTableB;
    this.rollupIdB = builder.rollupIdB;
    this.rollupCountB = builder.rollupCountB;
    this.rollupDisplayHintsB = builder.rollupDisplayHintsB;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UFTablePointer idPairsTable;
    private UFFieldPointer idPairsIdA;
    private UFFieldPointer idPairsIdB;

    private UFTablePointer rollupTableA;
    private UFFieldPointer rollupIdA;
    private UFFieldPointer rollupCountA;
    private UFFieldPointer rollupDisplayHintsA;

    private UFTablePointer rollupTableB;
    private UFFieldPointer rollupIdB;
    private UFFieldPointer rollupCountB;
    private UFFieldPointer rollupDisplayHintsB;

    public Builder idPairsTable(UFTablePointer idPairsTable) {
      this.idPairsTable = idPairsTable;
      return this;
    }

    public Builder idPairsIdA(UFFieldPointer idPairsIdA) {
      this.idPairsIdA = idPairsIdA;
      return this;
    }

    public Builder idPairsIdB(UFFieldPointer idPairsIdB) {
      this.idPairsIdB = idPairsIdB;
      return this;
    }

    public Builder rollupTableA(UFTablePointer rollupTableA) {
      this.rollupTableA = rollupTableA;
      return this;
    }

    public Builder rollupIdA(UFFieldPointer rollupIdA) {
      this.rollupIdA = rollupIdA;
      return this;
    }

    public Builder rollupCountA(UFFieldPointer rollupCountA) {
      this.rollupCountA = rollupCountA;
      return this;
    }

    public Builder rollupDisplayHintsA(UFFieldPointer rollupDisplayHintsA) {
      this.rollupDisplayHintsA = rollupDisplayHintsA;
      return this;
    }

    public Builder rollupTableB(UFTablePointer rollupTableB) {
      this.rollupTableB = rollupTableB;
      return this;
    }

    public Builder rollupIdB(UFFieldPointer rollupIdB) {
      this.rollupIdB = rollupIdB;
      return this;
    }

    public Builder rollupCountB(UFFieldPointer rollupCountB) {
      this.rollupCountB = rollupCountB;
      return this;
    }

    public Builder rollupDisplayHintsB(UFFieldPointer rollupDisplayHintsB) {
      this.rollupDisplayHintsB = rollupDisplayHintsB;
      return this;
    }

    /** Call the private constructor. */
    public UFRelationshipMapping build() {
      return new UFRelationshipMapping(this);
    }
  }

  public UFTablePointer getIdPairsTable() {
    return idPairsTable;
  }

  public UFFieldPointer getIdPairsIdA() {
    return idPairsIdA;
  }

  public UFFieldPointer getIdPairsIdB() {
    return idPairsIdB;
  }

  public UFTablePointer getRollupTableA() {
    return rollupTableA;
  }

  public UFFieldPointer getRollupIdA() {
    return rollupIdA;
  }

  public UFFieldPointer getRollupCountA() {
    return rollupCountA;
  }

  public UFFieldPointer getRollupDisplayHintsA() {
    return rollupDisplayHintsA;
  }

  public UFTablePointer getRollupTableB() {
    return rollupTableB;
  }

  public UFFieldPointer getRollupIdB() {
    return rollupIdB;
  }

  public UFFieldPointer getRollupCountB() {
    return rollupCountB;
  }

  public UFFieldPointer getRollupDisplayHintsB() {
    return rollupDisplayHintsB;
  }
}
