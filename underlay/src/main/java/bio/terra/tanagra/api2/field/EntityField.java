package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.Entity;
import java.util.List;

public abstract class EntityField {
  protected Entity entity;

  protected EntityField(Entity entity) {
    this.entity = entity;
  }

  public Entity getEntity() {
    return entity;
  }

  public abstract List<FieldVariable> buildFieldVariables(
      TableVariable entityTableVar, List<TableVariable> tableVars);

  public abstract List<ColumnSchema> getColumnSchemas();

  public abstract ValueDisplay parseFromRowResult(RowResult rowResult);
}
