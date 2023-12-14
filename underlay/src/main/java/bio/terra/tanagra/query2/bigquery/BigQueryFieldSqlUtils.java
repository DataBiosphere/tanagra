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
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public final class BigQueryFieldSqlUtils {
  private BigQueryFieldSqlUtils() {}

  public static List<Pair<FieldPointer, String>> getFieldsAndAliases(
      Underlay underlay,
      Entity entity,
      ValueDisplayField valueDisplayField,
      boolean includeValueField,
      boolean includeDisplayField) {
    ITEntityMain indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());

    if (valueDisplayField instanceof AttributeField) {
      return getFieldsAndAliases(
          (AttributeField) valueDisplayField, indexTable, includeValueField, includeDisplayField);
    } else if (valueDisplayField instanceof EntityIdCountField) {
      return getFieldsAndAliases((EntityIdCountField) valueDisplayField, indexTable);
    } else if (valueDisplayField instanceof HierarchyIsMemberField) {
      return getFieldsAndAliases((HierarchyIsMemberField) valueDisplayField, indexTable);
    } else if (valueDisplayField instanceof HierarchyIsRootField) {
      return getFieldsAndAliases((HierarchyIsRootField) valueDisplayField, indexTable);
    } else if (valueDisplayField instanceof HierarchyNumChildrenField) {
      return getFieldsAndAliases((HierarchyNumChildrenField) valueDisplayField, indexTable);
    } else if (valueDisplayField instanceof HierarchyPathField) {
      return getFieldsAndAliases((HierarchyPathField) valueDisplayField, indexTable);
    } else if (valueDisplayField instanceof RelatedEntityIdCountField) {
      return getFieldsAndAliases((RelatedEntityIdCountField) valueDisplayField, indexTable);
    } else {
      throw new SystemException(
          "Unsupported value display field type: " + valueDisplayField.getClass().getSimpleName());
    }
  }

  private static List<Pair<FieldPointer, String>> getFieldsAndAliases(
      AttributeField attributeField,
      ITEntityMain indexTable,
      boolean includeValueField,
      boolean includeDisplayField) {
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

  private static List<Pair<FieldPointer, String>> getFieldsAndAliases(
      EntityIdCountField entityIdCountField, ITEntityMain indexTable) {
    final String countFnStr = "COUNT";
    FieldPointer field =
        indexTable
            .getAttributeValueField(entityIdCountField.getIdAttribute().getName())
            .toBuilder()
            .sqlFunctionWrapper(countFnStr)
            .build();
    return List.of(Pair.of(field, entityIdCountField.getFieldAlias()));
  }

  private static List<Pair<FieldPointer, String>> getFieldsAndAliases(
      HierarchyIsMemberField hierarchyIsMemberField, ITEntityMain indexTable) {
    final String isMemberFnStr = "(${fieldSql} IS NOT NULL)";
    FieldPointer field =
        indexTable
            .getHierarchyPathField(hierarchyIsMemberField.getHierarchy().getName())
            .toBuilder()
            .sqlFunctionWrapper(isMemberFnStr)
            .build();
    return List.of(Pair.of(field, hierarchyIsMemberField.getFieldAlias()));
  }

  private static List<Pair<FieldPointer, String>> getFieldsAndAliases(
      HierarchyIsRootField hierarchyIsRootField, ITEntityMain indexTable) {
    final String isRootFnStr = "(${fieldSql} IS NOT NULL AND ${fieldSql}='')";
    FieldPointer field =
        indexTable
            .getHierarchyPathField(hierarchyIsRootField.getHierarchy().getName())
            .toBuilder()
            .sqlFunctionWrapper(isRootFnStr)
            .build();
    return List.of(Pair.of(field, hierarchyIsRootField.getFieldAlias()));
  }

  private static List<Pair<FieldPointer, String>> getFieldsAndAliases(
      HierarchyNumChildrenField hierarchyNumChildrenField, ITEntityMain indexTable) {
    FieldPointer field =
        indexTable.getHierarchyNumChildrenField(hierarchyNumChildrenField.getHierarchy().getName());
    return List.of(Pair.of(field, hierarchyNumChildrenField.getFieldAlias()));
  }

  private static List<Pair<FieldPointer, String>> getFieldsAndAliases(
      HierarchyPathField hierarchyPathField, ITEntityMain indexTable) {
    FieldPointer field =
        indexTable.getHierarchyPathField(hierarchyPathField.getHierarchy().getName());
    return List.of(Pair.of(field, hierarchyPathField.getFieldAlias()));
  }

  private static List<Pair<FieldPointer, String>> getFieldsAndAliases(
      RelatedEntityIdCountField relatedEntityIdCountField, ITEntityMain indexTable) {
    FieldPointer field =
        indexTable.getEntityGroupCountField(
            relatedEntityIdCountField.getEntityGroup().getName(),
            relatedEntityIdCountField.hasHierarchy()
                ? relatedEntityIdCountField.getHierarchy().getName()
                : null);
    return List.of(Pair.of(field, relatedEntityIdCountField.getFieldAlias()));
  }
}
