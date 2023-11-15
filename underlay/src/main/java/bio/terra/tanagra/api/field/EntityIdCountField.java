package bio.terra.tanagra.api.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;

public class EntityIdCountField extends SingleColumnField {
  private static final String FIELD_ALIAS = "IDCT";
  private final ITEntityMain indexTable;
  private final Attribute idAttribute;

  public EntityIdCountField(Underlay underlay, Entity entity) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.idAttribute = entity.getIdAttribute();
  }

  @Override
  protected FieldPointer getField() {
    return indexTable
        .getAttributeValueField(idAttribute.getName())
        .toBuilder()
        .sqlFunctionWrapper("COUNT")
        .build();
  }

  @Override
  protected String getFieldAlias() {
    return NameHelper.getReservedFieldName(FIELD_ALIAS);
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return CellValue.SQLDataType.INT64;
  }
}
