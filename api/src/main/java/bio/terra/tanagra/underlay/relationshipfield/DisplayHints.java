package bio.terra.tanagra.underlay.relationshipfield;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.RelationshipField;

public class DisplayHints extends RelationshipField {
  public DisplayHints(Entity entity) {
    super(entity);
  }

  @Override
  public Type getType() {
    return Type.DISPLAY_HINTS;
  }

  @Override
  public ColumnSchema buildColumnSchema() {
    return new ColumnSchema(getFieldAlias(getEntity()), CellValue.SQLDataType.STRING);
  }
}
