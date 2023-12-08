package bio.terra.tanagra.api.field.valuedisplay;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;

public class HierarchyIsRootField extends SingleColumnField {
  private static final String FIELD_ALIAS = "ISRT";

  private final ITEntityMain indexTable;
  private final Hierarchy hierarchy;

  public HierarchyIsRootField(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.hierarchy = hierarchy;
  }

  @Override
  protected FieldPointer getField() {
    // This is a calculated field. IS_ROOT means path IS NOT NULL AND path=''.
    return indexTable
        .getHierarchyPathField(hierarchy.getName())
        .toBuilder()
        .sqlFunctionWrapper("(${fieldSql} IS NOT NULL AND ${fieldSql}='')")
        .build();
  }

  @Override
  public String getFieldAlias() {
    return NameHelper.getReservedFieldName(FIELD_ALIAS + "_" + hierarchy.getName());
  }

  @Override
  protected CellValue.SQLDataType getFieldDataType() {
    return CellValue.SQLDataType.BOOLEAN;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }
}
