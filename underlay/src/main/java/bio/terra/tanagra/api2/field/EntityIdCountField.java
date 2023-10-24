package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.indexschema.SchemaUtils;

public class EntityIdCountField extends SingleColumnField {
  private static final String FIELD_ALIAS = "IDCT";

  public EntityIdCountField(Entity entity) {
    super(entity);
  }

  @Override
  protected FieldPointer getField() {
    return entity
        .getIdAttribute()
        .getIndexValueField()
        .toBuilder()
        .sqlFunctionWrapper("COUNT")
        .build();
  }

  @Override
  protected String getFieldAlias() {
    return SchemaUtils.getReservedFieldName(FIELD_ALIAS);
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return CellValue.SQLDataType.INT64;
  }
}
