package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay2.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;

public class RelatedEntityIdCountField extends SingleColumnField {
  private final ITEntityMain indexTable;
  private final EntityGroup entityGroup;
  private final Hierarchy hierarchy;

  protected RelatedEntityIdCountField(
      Underlay underlay, Entity countForEntity, EntityGroup entityGroup, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(countForEntity.getName());
    this.entityGroup = entityGroup;
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    return indexTable.getEntityGroupCountField(
        entityGroup.getName(), hierarchy == null ? null : hierarchy.getName());
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return CellValue.SQLDataType.INT64;
  }
}
