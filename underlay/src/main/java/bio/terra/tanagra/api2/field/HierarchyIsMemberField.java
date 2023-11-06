package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.NameHelper;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;

public class HierarchyIsMemberField extends SingleColumnField {
  private static final String FIELD_ALIAS = "ISMEM";
  private final ITEntityMain indexTable;
  private final Hierarchy hierarchy;

  protected HierarchyIsMemberField(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    // This is a calculated field. IS_MEMBER means path IS NOT NULL.
    return indexTable
        .getHierarchyPathField(hierarchy.getName())
        .toBuilder()
        .sqlFunctionWrapper("(${fieldSql} IS NOT NULL)")
        .build();
  }

  @Override
  protected String getFieldAlias() {
    return NameHelper.getReservedFieldName(FIELD_ALIAS + "_" + hierarchy.getName());
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return CellValue.SQLDataType.BOOLEAN;
  }
}
