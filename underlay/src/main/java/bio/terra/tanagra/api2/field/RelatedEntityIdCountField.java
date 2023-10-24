package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Hierarchy;
import bio.terra.tanagra.underlay2.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;

public class RelatedEntityIdCountField extends SingleColumnField {
  private final Entity countedEntity;
  private final EntityGroup entityGroup;
  private final Hierarchy hierarchy;

  protected RelatedEntityIdCountField(
      Entity countForEntity, Entity countedEntity, EntityGroup entityGroup, Hierarchy hierarchy) {
    super(countForEntity);
    this.countedEntity = countedEntity;
    this.entityGroup = entityGroup;
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    return entityGroup.getCountField(
        entity.getName(), countedEntity.getName(), hierarchy == null ? null : hierarchy.getName());
  }

  @Override
  protected String getFieldAlias() {
    return EntityMain.getEntityGroupCountFieldName(
        entity.getName(), hierarchy == null ? null : hierarchy.getName(), entityGroup.getName());
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return EntityMain.ENTITY_GROUP_COUNT_SQL_TYPE;
  }
}
