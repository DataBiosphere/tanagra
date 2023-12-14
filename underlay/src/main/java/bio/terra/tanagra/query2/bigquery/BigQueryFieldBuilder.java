package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public final class BigQueryFieldBuilder {
  private final ITEntityMain indexTable;
  private final boolean includeValueField;
  private final boolean includeDisplayField;

  private BigQueryFieldBuilder(
      ITEntityMain indexTable, boolean includeValueField, boolean includeDisplayField) {
    this.indexTable = indexTable;
    this.includeValueField = includeValueField;
    this.includeDisplayField = includeDisplayField;
  }

  public static BigQueryFieldBuilder includeValueOnly(ITEntityMain indexTable) {
    return new BigQueryFieldBuilder(indexTable, true, false);
  }

  public static BigQueryFieldBuilder includeDisplayOnly(ITEntityMain indexTable) {
    return new BigQueryFieldBuilder(indexTable, false, true);
  }

  public static BigQueryFieldBuilder includeBothValueAndDisplay(ITEntityMain indexTable) {
    return new BigQueryFieldBuilder(indexTable, true, true);
  }

  public List<Pair<FieldPointer, String>> getFieldsAndAliases(ValueDisplayField valueDisplayField) {
    if (valueDisplayField instanceof AttributeField) {
      return getFieldsAndAliases((AttributeField) valueDisplayField);
    } else if (valueDisplayField instanceof EntityIdCountField) {
      return getFieldsAndAliases((EntityIdCountField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyIsMemberField) {
      return getFieldsAndAliases((HierarchyIsMemberField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyIsRootField) {
      return getFieldsAndAliases((HierarchyIsRootField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyNumChildrenField) {
      return getFieldsAndAliases((HierarchyNumChildrenField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyPathField) {
      return getFieldsAndAliases((HierarchyPathField) valueDisplayField);
    } else if (valueDisplayField instanceof RelatedEntityIdCountField) {
      return getFieldsAndAliases((RelatedEntityIdCountField) valueDisplayField);
    } else {
      throw new SystemException(
          "Unsupported value display field type: " + valueDisplayField.getClass().getSimpleName());
    }
  }

  private List<Pair<FieldPointer, String>> getFieldsAndAliases(AttributeField attributeField) {
    Attribute attribute = attributeField.getAttribute();
    FieldPointer valueField = indexTable.getAttributeValueField(attribute.getName());
    if (attribute.hasRuntimeSqlFunctionWrapper()) {
      valueField =
          valueField
              .toBuilder()
              .sqlFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper())
              .build();
    }

    Pair<FieldPointer, String> valueFieldAndAlias =
        Pair.of(valueField, attributeField.getValueFieldAlias());
    if (attribute.isSimple() || attributeField.isExcludeDisplay()) {
      return List.of(valueFieldAndAlias);
    }

    FieldPointer displayField = indexTable.getAttributeDisplayField(attribute.getName());
    Pair<FieldPointer, String> displayFieldAndAlias =
        Pair.of(displayField, attributeField.getDisplayFieldAlias());
    List<Pair<FieldPointer, String>> fieldsAndAliases = new ArrayList<>();
    if (includeValueField) {
      fieldsAndAliases.add(valueFieldAndAlias);
    }
    if (includeDisplayField) {
      fieldsAndAliases.add(displayFieldAndAlias);
    }
    return fieldsAndAliases;
  }

  private List<Pair<FieldPointer, String>> getFieldsAndAliases(
      EntityIdCountField entityIdCountField) {
    final String countFnStr = "COUNT";
    FieldPointer field =
        indexTable
            .getAttributeValueField(entityIdCountField.getIdAttribute().getName())
            .toBuilder()
            .sqlFunctionWrapper(countFnStr)
            .build();
    return List.of(Pair.of(field, entityIdCountField.getFieldAlias()));
  }

  private List<Pair<FieldPointer, String>> getFieldsAndAliases(
      HierarchyIsMemberField hierarchyIsMemberField) {
    final String isMemberFnStr = "(${fieldSql} IS NOT NULL)";
    FieldPointer field =
        indexTable
            .getHierarchyPathField(hierarchyIsMemberField.getHierarchy().getName())
            .toBuilder()
            .sqlFunctionWrapper(isMemberFnStr)
            .build();
    return List.of(Pair.of(field, hierarchyIsMemberField.getFieldAlias()));
  }

  private List<Pair<FieldPointer, String>> getFieldsAndAliases(
      HierarchyIsRootField hierarchyIsRootField) {
    final String isRootFnStr = "(${fieldSql} IS NOT NULL AND ${fieldSql}='')";
    FieldPointer field =
        indexTable
            .getHierarchyPathField(hierarchyIsRootField.getHierarchy().getName())
            .toBuilder()
            .sqlFunctionWrapper(isRootFnStr)
            .build();
    return List.of(Pair.of(field, hierarchyIsRootField.getFieldAlias()));
  }

  private List<Pair<FieldPointer, String>> getFieldsAndAliases(
      HierarchyNumChildrenField hierarchyNumChildrenField) {
    FieldPointer field =
        indexTable.getHierarchyNumChildrenField(hierarchyNumChildrenField.getHierarchy().getName());
    return List.of(Pair.of(field, hierarchyNumChildrenField.getFieldAlias()));
  }

  private List<Pair<FieldPointer, String>> getFieldsAndAliases(
      HierarchyPathField hierarchyPathField) {
    FieldPointer field =
        indexTable.getHierarchyPathField(hierarchyPathField.getHierarchy().getName());
    return List.of(Pair.of(field, hierarchyPathField.getFieldAlias()));
  }

  private List<Pair<FieldPointer, String>> getFieldsAndAliases(
      RelatedEntityIdCountField relatedEntityIdCountField) {
    FieldPointer field =
        indexTable.getEntityGroupCountField(
            relatedEntityIdCountField.getEntityGroup().getName(),
            relatedEntityIdCountField.hasHierarchy()
                ? relatedEntityIdCountField.getHierarchy().getName()
                : null);
    return List.of(Pair.of(field, relatedEntityIdCountField.getFieldAlias()));
  }
}
