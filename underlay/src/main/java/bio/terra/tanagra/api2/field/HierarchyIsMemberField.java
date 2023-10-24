package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Hierarchy;
import bio.terra.tanagra.underlay2.indexschema.SchemaUtils;

public class HierarchyIsMemberField extends SingleColumnField {
  private static final String FIELD_ALIAS = "ISMEM";
  private final Hierarchy hierarchy;

  protected HierarchyIsMemberField(Entity entity, Hierarchy hierarchy) {
    super(entity);
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    // This is a calculated field. IS_MEMBER means path IS NOT NULL.
    return hierarchy
        .getIndexPathField()
        .toBuilder()
        .sqlFunctionWrapper("(${fieldSql} IS NOT NULL)")
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
