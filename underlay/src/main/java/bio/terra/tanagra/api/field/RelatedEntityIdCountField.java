package bio.terra.tanagra.api.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import javax.annotation.Nullable;

public class RelatedEntityIdCountField extends SingleColumnField {
  private final ITEntityMain indexTable;
  private final Entity countedEntity;
  private final EntityGroup entityGroup;
  private final @Nullable Hierarchy hierarchy;

  public RelatedEntityIdCountField(
      Underlay underlay,
      Entity countForEntity,
      Entity countedEntity,
      EntityGroup entityGroup,
      @Nullable Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(countForEntity.getName());
    this.countedEntity = countedEntity;
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

  public Entity getCountedEntity() {
    return countedEntity;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }
}
