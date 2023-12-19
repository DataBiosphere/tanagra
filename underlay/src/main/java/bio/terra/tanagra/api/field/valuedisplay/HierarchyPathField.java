package bio.terra.tanagra.api.field.valuedisplay;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;

public class HierarchyPathField extends SingleColumnField {
  private final ITEntityMain indexTable;
  private final Underlay underlay;
  private final Entity entity;
  private final Hierarchy hierarchy;

  public HierarchyPathField(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.underlay = underlay;
    this.entity = entity;
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

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }
}
