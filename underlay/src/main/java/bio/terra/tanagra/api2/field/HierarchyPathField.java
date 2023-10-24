package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Hierarchy;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;

public class HierarchyPathField extends SingleColumnField {
  private final Hierarchy hierarchy;

  protected HierarchyPathField(Entity entity, Hierarchy hierarchy) {
    super(entity);
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    return hierarchy.getIndexPathField();
  }

  @Override
  protected String getFieldAlias() {
    return EntityMain.getHierarchyPathFieldName(getEntity().getName(), hierarchy.getName());
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return EntityMain.HIERARCHY_PATH_SQL_TYPE;
  }
}
