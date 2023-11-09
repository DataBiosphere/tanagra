package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import java.util.*;

public final class RelationshipMapping {
  public static final String COUNT_FIELD_PREFIX = "count_";
  public static final String DISPLAY_HINTS_FIELD_PREFIX = "displayhints_";
  private static final String ID_PAIRS_TABLE_SUFFIX = "_idpairs";
  private static final String ID_FIELD_NAME_PREFIX = "id_";
  public static final String NO_HIERARCHY_KEY = "NO_HIERARCHY";
  private final String foreignKeyAttribute;
  private final FieldPointer idPairsIdA;
  private final FieldPointer idPairsIdB;
  private final Map<String, RollupInformation> rollupInformationMapA;
  private final Map<String, RollupInformation> rollupInformationMapB;

  private Relationship relationship;

  private RelationshipMapping(
      String foreignKeyAttribute,
      FieldPointer idPairsIdA,
      FieldPointer idPairsIdB,
      Map<String, RollupInformation> rollupInformationMapA,
      Map<String, RollupInformation> rollupInformationMapB) {
    this.foreignKeyAttribute = foreignKeyAttribute;
    this.idPairsIdA = idPairsIdA;
    this.idPairsIdB = idPairsIdB;
    this.rollupInformationMapA = rollupInformationMapA;
    this.rollupInformationMapB = rollupInformationMapB;
  }

  public void initialize(Relationship relationship) {
    this.relationship = relationship;
  }

  public static RelationshipMapping fromSerialized(
      UFRelationshipMapping serialized, DataPointer dataPointer, Relationship relationship) {
    // ID pairs table.
    FieldPointer idPairsIdA;
    FieldPointer idPairsIdB;
    if (serialized.getForeignKeyAttribute() == null) {
      TablePointer idPairsTable =
          TablePointer.fromSerialized(serialized.getIdPairsTable(), dataPointer);
      idPairsIdA = FieldPointer.fromSerialized(serialized.getIdPairsIdA(), idPairsTable);
      idPairsIdB = FieldPointer.fromSerialized(serialized.getIdPairsIdB(), idPairsTable);
    } else {
      idPairsIdA =
          relationship
              .getEntityA()
              .getIdAttribute()
              .getMapping(Underlay.MappingType.SOURCE)
              .getValue();
      idPairsIdB =
          relationship
              .getEntityA()
              .getAttribute(serialized.getForeignKeyAttribute())
              .getMapping(Underlay.MappingType.SOURCE)
              .getValue();
    }

    // Rollup columns for entity A.
    Map<String, RollupInformation> rollupInformationMapA = new HashMap<>();
    if (serialized.getRollupInformationMapA() != null) {
      serialized.getRollupInformationMapA().entrySet().stream()
          .forEach(
              entry ->
                  rollupInformationMapA.put(
                      entry.getKey(),
                      RollupInformation.fromSerialized(entry.getValue(), dataPointer)));
    }

    // Rollup columns for entity B.
    Map<String, RollupInformation> rollupInformationMapB = new HashMap<>();
    if (serialized.getRollupInformationMapB() != null) {
      serialized.getRollupInformationMapB().entrySet().stream()
          .forEach(
              entry ->
                  rollupInformationMapB.put(
                      entry.getKey(),
                      RollupInformation.fromSerialized(entry.getValue(), dataPointer)));
    }

    return new RelationshipMapping(
        serialized.getForeignKeyAttribute(),
        idPairsIdA,
        idPairsIdB,
        rollupInformationMapA,
        rollupInformationMapB);
  }

  public static RelationshipMapping defaultIndexMapping(
      DataPointer dataPointer,
      Relationship relationship,
      String entityGroupName,
      RelationshipMapping sourceMapping) {
    // ID pairs table.
    FieldPointer idPairsIdA;
    FieldPointer idPairsIdB;
    if (sourceMapping.getForeignKeyAttribute() == null) {
      TablePointer idPairsTable =
          TablePointer.fromTableName(
              entityGroupName
                  + "_"
                  + relationship.getEntityA().getName()
                  + "_"
                  + relationship.getEntityB().getName()
                  + ID_PAIRS_TABLE_SUFFIX,
              dataPointer);
      idPairsIdA =
          new FieldPointer.Builder()
              .tablePointer(idPairsTable)
              .columnName(ID_FIELD_NAME_PREFIX + relationship.getEntityA().getName())
              .build();
      idPairsIdB =
          new FieldPointer.Builder()
              .tablePointer(idPairsTable)
              .columnName(ID_FIELD_NAME_PREFIX + relationship.getEntityB().getName())
              .build();
    } else {
      idPairsIdA =
          relationship
              .getEntityA()
              .getIdAttribute()
              .getMapping(Underlay.MappingType.INDEX)
              .getValue();
      idPairsIdB =
          relationship
              .getEntityA()
              .getAttribute(sourceMapping.getForeignKeyAttribute())
              .getMapping(Underlay.MappingType.INDEX)
              .getValue();
    }

    // Rollup columns in entity A table.
    Map<String, RollupInformation> rollupInformationMapA = new HashMap<>();
    rollupInformationMapA.put(
        NO_HIERARCHY_KEY,
        RollupInformation.defaultIndexMapping(
            relationship.getEntityA(), relationship.getEntityB(), null));
    if (relationship.getEntityA().hasHierarchies()) {
      relationship.getEntityA().getHierarchies().stream()
          .forEach(
              hierarchy ->
                  rollupInformationMapA.put(
                      hierarchy.getName(),
                      RollupInformation.defaultIndexMapping(
                          relationship.getEntityA(), relationship.getEntityB(), hierarchy)));
    }

    // Rollup columns in entity B table.
    Map<String, RollupInformation> rollupInformationMapB = new HashMap<>();
    rollupInformationMapB.put(
        NO_HIERARCHY_KEY,
        RollupInformation.defaultIndexMapping(
            relationship.getEntityB(), relationship.getEntityA(), null));
    if (relationship.getEntityB().hasHierarchies()) {
      relationship.getEntityB().getHierarchies().stream()
          .forEach(
              hierarchy ->
                  rollupInformationMapB.put(
                      hierarchy.getName(),
                      RollupInformation.defaultIndexMapping(
                          relationship.getEntityB(), relationship.getEntityA(), hierarchy)));
    }

    return new RelationshipMapping(
        sourceMapping.getForeignKeyAttribute(),
        idPairsIdA,
        idPairsIdB,
        rollupInformationMapA,
        rollupInformationMapB);
  }

  public Query queryIdPairs(String idAAlias, String idBAlias) {
    TableVariable tableVariable = TableVariable.forPrimary(getIdPairsTable());
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

  public String getForeignKeyAttribute() {
    return foreignKeyAttribute;
  }

  public TablePointer getIdPairsTable() {
    return idPairsIdA.getTablePointer();
  }

  public FieldPointer getIdPairsIdA() {
    return idPairsIdA;
  }

  public FieldPointer getIdPairsIdB() {
    return idPairsIdB;
  }

  public RollupInformation getRollupInfo(Entity entity, Hierarchy hierarchy) {
    return relationship.getEntityA().equals(entity)
        ? getRollupInfoA(hierarchy)
        : getRollupInfoB(hierarchy);
  }

  public RollupInformation getRollupInfoA(Hierarchy hierarchy) {
    return rollupInformationMapA.get(hierarchy == null ? NO_HIERARCHY_KEY : hierarchy.getName());
  }

  public RollupInformation getRollupInfoB(Hierarchy hierarchy) {
    return rollupInformationMapB.get(hierarchy == null ? NO_HIERARCHY_KEY : hierarchy.getName());
  }

  public Map<String, RollupInformation> getRollupInformationMapA() {
    return Collections.unmodifiableMap(rollupInformationMapA);
  }

  public Map<String, RollupInformation> getRollupInformationMapB() {
    return Collections.unmodifiableMap(rollupInformationMapB);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RelationshipMapping that = (RelationshipMapping) o;
    return idPairsIdA.equals(that.idPairsIdA)
        && idPairsIdB.equals(that.idPairsIdB)
        && rollupInformationMapA.equals(that.rollupInformationMapA)
        && rollupInformationMapB.equals(that.rollupInformationMapB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(idPairsIdA, idPairsIdB, rollupInformationMapA, rollupInformationMapB);
  }
}