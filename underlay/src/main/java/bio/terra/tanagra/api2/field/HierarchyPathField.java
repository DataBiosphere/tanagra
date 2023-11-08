package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;

public class HierarchyPathField extends SingleColumnField {
  private final ITEntityMain indexTable;
  private final Hierarchy hierarchy;

  public HierarchyPathField(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    return indexTable.getHierarchyPathField(hierarchy.getName());
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return CellValue.SQLDataType.STRING;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }
}
