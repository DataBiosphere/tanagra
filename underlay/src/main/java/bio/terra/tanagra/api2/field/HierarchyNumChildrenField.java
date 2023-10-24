package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Hierarchy;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;

public class HierarchyNumChildrenField extends SingleColumnField {
  private final Hierarchy hierarchy;

  protected HierarchyNumChildrenField(Entity entity, Hierarchy hierarchy) {
    super(entity);
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    return hierarchy.getIndexNumChildrenField();
  }

  @Override
  protected String getFieldAlias() {
    return EntityMain.getHierarchyNumchildrenFieldName(getEntity().getName(), hierarchy.getName());
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return EntityMain.HIERARCHY_NUMCHILDREN_SQL_TYPE;
  }
}
