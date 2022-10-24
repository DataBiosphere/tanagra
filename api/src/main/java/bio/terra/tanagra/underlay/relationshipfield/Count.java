package bio.terra.tanagra.underlay.relationshipfield;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.RelationshipField;

public class Count extends RelationshipField {
  public Count(Entity entity) {
    super(entity);
  }

  @Override
  public Type getType() {
    return Type.COUNT;
  }

  @Override
  public ColumnSchema buildColumnSchema() {
    return new ColumnSchema(getFieldAlias(getEntity()), CellValue.SQLDataType.INT64);
  }
}
