package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RelationshipMapping {
  private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipMapping.class);

  public static final String COUNT_FIELD_PREFIX = "count_";
  public static final String DISPLAY_HINTS_FIELD_PREFIX = "displayhints_";
  private static final String ID_PAIRS_TABLE_PREFIX = "idpairs_";
  private static final String ID_FIELD_NAME_PREFIX = "id_";

  private final TablePointer idPairsTable;
  private final FieldPointer idPairsIdA;
  private final FieldPointer idPairsIdB;

  private final TablePointer rollupTableA;
  private final FieldPointer rollupIdA;
  private final FieldPointer rollupCountA;
  private final FieldPointer rollupDisplayHintsA;

  private final TablePointer rollupTableB;
  private final FieldPointer rollupIdB;
  private final FieldPointer rollupCountB;
  private final FieldPointer rollupDisplayHintsB;

  private Relationship relationship;

  private RelationshipMapping(Builder builder) {
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

  public void initialize(Relationship relationship) {
    this.relationship = relationship;
  }

  public static RelationshipMapping fromSerialized(
      UFRelationshipMapping serialized, DataPointer dataPointer) {
    // ID pairs table.
    TablePointer idPairsTable =
        TablePointer.fromSerialized(serialized.getIdPairsTable(), dataPointer);
    FieldPointer idPairsIdA = FieldPointer.fromSerialized(serialized.getIdPairsIdA(), idPairsTable);
    FieldPointer idPairsIdB = FieldPointer.fromSerialized(serialized.getIdPairsIdB(), idPairsTable);
    Builder builder =
        new Builder().idPairsTable(idPairsTable).idPairsIdA(idPairsIdA).idPairsIdB(idPairsIdB);

    // Rollup columns for entity A.
    if (serialized.getRollupTableA() != null) {
      TablePointer rollupTableA =
          TablePointer.fromSerialized(serialized.getRollupTableA(), dataPointer);
      FieldPointer rollupIdA = FieldPointer.fromSerialized(serialized.getRollupIdA(), rollupTableA);
      FieldPointer rollupCountA =
          FieldPointer.fromSerialized(serialized.getRollupCountA(), rollupTableA);
      FieldPointer rollupDisplayHintsA =
          FieldPointer.fromSerialized(serialized.getRollupDisplayHintsA(), rollupTableA);
      builder
          .rollupTableA(rollupTableA)
          .rollupIdA(rollupIdA)
          .rollupCountA(rollupCountA)
          .rollupDisplayHintsA(rollupDisplayHintsA);
    }

    // Rollup columns for entity B.
    if (serialized.getRollupTableB() != null) {
      TablePointer rollupTableB =
          TablePointer.fromSerialized(serialized.getRollupTableB(), dataPointer);
      FieldPointer rollupIdB = FieldPointer.fromSerialized(serialized.getRollupIdB(), rollupTableB);
      FieldPointer rollupCountB =
          FieldPointer.fromSerialized(serialized.getRollupCountB(), rollupTableB);
      FieldPointer rollupDisplayHintsB =
          FieldPointer.fromSerialized(serialized.getRollupDisplayHintsB(), rollupTableB);
      builder
          .rollupTableB(rollupTableB)
          .rollupIdB(rollupIdB)
          .rollupCountB(rollupCountB)
          .rollupDisplayHintsB(rollupDisplayHintsB);
    }

    return builder.build();
  }

  public static RelationshipMapping defaultIndexMapping(
      DataPointer dataPointer, Relationship relationship) {
    // ID pairs table.
    TablePointer idPairsTable =
        TablePointer.fromTableName(
            ID_PAIRS_TABLE_PREFIX
                + relationship.getEntityA().getName()
                + "_"
                + relationship.getEntityB().getName(),
            dataPointer);
    FieldPointer idPairsIdA =
        new FieldPointer.Builder()
            .tablePointer(idPairsTable)
            .columnName(ID_FIELD_NAME_PREFIX + relationship.getEntityA().getName())
            .build();
    FieldPointer idPairsIdB =
        new FieldPointer.Builder()
            .tablePointer(idPairsTable)
            .columnName(ID_FIELD_NAME_PREFIX + relationship.getEntityB().getName())
            .build();

    // Rollup columns in entity A table.
    TablePointer entityTableA =
        relationship.getEntityA().getMapping(Underlay.MappingType.INDEX).getTablePointer();
    FieldPointer rollupIdA =
        relationship
            .getEntityA()
            .getIdAttribute()
            .getMapping(Underlay.MappingType.INDEX)
            .getValue();
    FieldPointer rollupCountA =
        new FieldPointer.Builder()
            .tablePointer(entityTableA)
            .columnName(
                RelationshipField.getFieldAlias(
                    relationship.getEntityB().getName(), RelationshipField.Type.COUNT))
            .build();
    FieldPointer rollupDisplayHintsA =
        new FieldPointer.Builder()
            .tablePointer(entityTableA)
            .columnName(
                RelationshipField.getFieldAlias(
                    relationship.getEntityB().getName(), RelationshipField.Type.DISPLAY_HINTS))
            .build();

    // Rollup columns in entity B table.
    TablePointer entityTableB =
        relationship.getEntityB().getMapping(Underlay.MappingType.INDEX).getTablePointer();
    FieldPointer rollupIdB =
        relationship
            .getEntityB()
            .getIdAttribute()
            .getMapping(Underlay.MappingType.INDEX)
            .getValue();
    FieldPointer rollupCountB =
        new FieldPointer.Builder()
            .tablePointer(entityTableB)
            .columnName(
                RelationshipField.getFieldAlias(
                    relationship.getEntityA().getName(), RelationshipField.Type.COUNT))
            .build();
    FieldPointer rollupDisplayHintsB =
        new FieldPointer.Builder()
            .tablePointer(entityTableB)
            .columnName(
                RelationshipField.getFieldAlias(
                    relationship.getEntityA().getName(), RelationshipField.Type.DISPLAY_HINTS))
            .build();

    return new Builder()
        .idPairsTable(idPairsTable)
        .idPairsIdA(idPairsIdA)
        .idPairsIdB(idPairsIdB)
        .rollupTableA(entityTableA)
        .rollupIdA(rollupIdA)
        .rollupCountA(rollupCountA)
        .rollupDisplayHintsA(rollupDisplayHintsA)
        .rollupTableB(entityTableB)
        .rollupIdB(rollupIdB)
        .rollupCountB(rollupCountB)
        .rollupDisplayHintsB(rollupDisplayHintsB)
        .build();
  }

  public Query queryIdPairs(String idAAlias, String idBAlias) {
    TableVariable tableVariable = TableVariable.forPrimary(idPairsTable);
    FieldVariable idAFieldVar = new FieldVariable(getIdPairsIdA(), tableVariable, idAAlias);
    FieldVariable idBFieldVar = new FieldVariable(getIdPairsIdB(), tableVariable, idBAlias);
    return new Query.Builder()
        .select(List.of(idAFieldVar, idBFieldVar))
        .tables(List.of(tableVariable))
        .build();
  }

  public FieldPointer getIdPairsId(Entity entity) {
    return (relationship.getEntityA().equals(entity)) ? getIdPairsIdA() : getIdPairsIdB();
  }

  public TablePointer getRollupTable(Entity entity) {
    return (relationship.getEntityA().equals(entity)) ? getRollupTableA() : getRollupTableB();
  }

  public FieldPointer getRollupId(Entity entity) {
    return (relationship.getEntityA().equals(entity)) ? getRollupIdA() : getRollupIdB();
  }

  public FieldPointer getRollupCount(Entity entity) {
    return (relationship.getEntityA().equals(entity)) ? getRollupCountA() : getRollupCountB();
  }

  public FieldPointer getRollupDisplayHints(Entity entity) {
    return (relationship.getEntityA().equals(entity))
        ? getRollupDisplayHintsA()
        : getRollupDisplayHintsB();
  }

  public TablePointer getIdPairsTable() {
    return idPairsTable;
  }

  public FieldPointer getIdPairsIdA() {
    return idPairsIdA;
  }

  public FieldPointer getIdPairsIdB() {
    return idPairsIdB;
  }

  public TablePointer getRollupTableA() {
    return rollupTableA;
  }

  public FieldPointer getRollupIdA() {
    return rollupIdA;
  }

  public FieldPointer getRollupCountA() {
    return rollupCountA;
  }

  public FieldPointer getRollupDisplayHintsA() {
    return rollupDisplayHintsA;
  }

  public TablePointer getRollupTableB() {
    return rollupTableB;
  }

  public FieldPointer getRollupIdB() {
    return rollupIdB;
  }

  public FieldPointer getRollupCountB() {
    return rollupCountB;
  }

  public FieldPointer getRollupDisplayHintsB() {
    return rollupDisplayHintsB;
  }

  public static class Builder {
    private TablePointer idPairsTable;
    private FieldPointer idPairsIdA;
    private FieldPointer idPairsIdB;

    private TablePointer rollupTableA;
    private FieldPointer rollupIdA;
    private FieldPointer rollupCountA;
    private FieldPointer rollupDisplayHintsA;

    private TablePointer rollupTableB;
    private FieldPointer rollupIdB;
    private FieldPointer rollupCountB;
    private FieldPointer rollupDisplayHintsB;

    public Builder idPairsTable(TablePointer idPairsTable) {
      this.idPairsTable = idPairsTable;
      return this;
    }

    public Builder idPairsIdA(FieldPointer idPairsIdA) {
      this.idPairsIdA = idPairsIdA;
      return this;
    }

    public Builder idPairsIdB(FieldPointer idPairsIdB) {
      this.idPairsIdB = idPairsIdB;
      return this;
    }

    public Builder rollupTableA(TablePointer rollupTableA) {
      this.rollupTableA = rollupTableA;
      return this;
    }

    public Builder rollupIdA(FieldPointer rollupIdA) {
      this.rollupIdA = rollupIdA;
      return this;
    }

    public Builder rollupCountA(FieldPointer rollupCountA) {
      this.rollupCountA = rollupCountA;
      return this;
    }

    public Builder rollupDisplayHintsA(FieldPointer rollupDisplayHintsA) {
      this.rollupDisplayHintsA = rollupDisplayHintsA;
      return this;
    }

    public Builder rollupTableB(TablePointer rollupTableB) {
      this.rollupTableB = rollupTableB;
      return this;
    }

    public Builder rollupIdB(FieldPointer rollupIdB) {
      this.rollupIdB = rollupIdB;
      return this;
    }

    public Builder rollupCountB(FieldPointer rollupCountB) {
      this.rollupCountB = rollupCountB;
      return this;
    }

    public Builder rollupDisplayHintsB(FieldPointer rollupDisplayHintsB) {
      this.rollupDisplayHintsB = rollupDisplayHintsB;
      return this;
    }

    public RelationshipMapping build() {
      return new RelationshipMapping(this);
    }
  }
}
