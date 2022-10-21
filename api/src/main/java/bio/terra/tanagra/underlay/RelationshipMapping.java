package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import java.util.List;

public final class RelationshipMapping {
  private final TablePointer tablePointer;
  private final FieldPointer fromEntityId;
  private final FieldPointer toEntityId;

  private RelationshipMapping(
      TablePointer tablePointer, FieldPointer fromEntityId, FieldPointer toEntityId) {
    this.tablePointer = tablePointer;
    this.fromEntityId = fromEntityId;
    this.toEntityId = toEntityId;
  }

  public static RelationshipMapping fromSerialized(
      UFRelationshipMapping serialized, DataPointer dataPointer) {
    TablePointer tablePointer =
        TablePointer.fromSerialized(serialized.getTablePointer(), dataPointer);
    FieldPointer fromEntityId =
        FieldPointer.fromSerialized(serialized.getFromEntityId(), tablePointer);
    FieldPointer toEntityId = FieldPointer.fromSerialized(serialized.getToEntityId(), tablePointer);
    return new RelationshipMapping(tablePointer, fromEntityId, toEntityId);
  }

  public Query queryIdPairs(String fromEntityAlias, String toEntityAlias) {
    TableVariable tableVariable = TableVariable.forPrimary(tablePointer);
    FieldVariable fromEntityIdFieldVariable =
        new FieldVariable(fromEntityId, tableVariable, fromEntityAlias);
    FieldVariable toEntityIdFieldVariable =
        new FieldVariable(toEntityId, tableVariable, toEntityAlias);
    return new Query.Builder()
        .select(List.of(fromEntityIdFieldVariable, toEntityIdFieldVariable))
        .tables(List.of(tableVariable))
        .build();
  }

  public TablePointer getTablePointer() {
    return tablePointer;
  }

  public FieldPointer getFromEntityId() {
    return fromEntityId;
  }

  public FieldPointer getToEntityId() {
    return toEntityId;
  }
}
