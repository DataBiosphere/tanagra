package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Hierarchy;
import bio.terra.tanagra.underlay2.indexschema.SchemaUtils;

public class HierarchyIsRootField extends SingleColumnField {
  private static final String FIELD_ALIAS = "ISRT";

  private final Hierarchy hierarchy;

  protected HierarchyIsRootField(Entity entity, Hierarchy hierarchy) {
    super(entity);
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    // This is a calculated field. IS_ROOT means path IS NOT NULL AND path=''.
    return hierarchy
        .getIndexPathField()
        .toBuilder()
        .sqlFunctionWrapper("(${fieldSql} IS NOT NULL AND ${fieldSql}='')")
        .build();
  }

  @Override
  protected String getFieldAlias() {
    return SchemaUtils.getReservedFieldName(FIELD_ALIAS + "_" + hierarchy);
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return CellValue.SQLDataType.BOOLEAN;
  }
}
