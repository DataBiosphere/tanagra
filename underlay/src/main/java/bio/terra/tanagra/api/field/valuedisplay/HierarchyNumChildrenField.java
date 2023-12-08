package bio.terra.tanagra.api.field.valuedisplay;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;

public class HierarchyNumChildrenField extends SingleColumnField {
  private final ITEntityMain indexTable;
  private final Hierarchy hierarchy;

  public HierarchyNumChildrenField(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    return indexTable.getHierarchyNumChildrenField(hierarchy.getName());
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return CellValue.SQLDataType.INT64;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }
}
