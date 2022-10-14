package bio.terra.tanagra.api;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityInstance {
  private final Map<Attribute, ValueDisplay> attributeValues;
  private final Map<HierarchyField, ValueDisplay> hierarchyFieldValues;

  private EntityInstance(
      Map<Attribute, ValueDisplay> attributeValues,
      Map<HierarchyField, ValueDisplay> hierarchyFieldValues) {
    this.attributeValues = attributeValues;
    this.hierarchyFieldValues = hierarchyFieldValues;
  }

  public static EntityInstance fromRowResult(
      RowResult rowResult,
      List<Attribute> selectedAttributes,
      List<HierarchyField> selectedHierarchyFields) {
    Map<Attribute, ValueDisplay> attributeValues = new HashMap<>();
    for (Attribute selectedAttribute : selectedAttributes) {
      CellValue cellValue = rowResult.get(selectedAttribute.getName());
      if (cellValue == null) {
        throw new SystemException("Attribute column not found: " + selectedAttribute.getName());
      }

      Literal value = cellValue.getLiteral();
      switch (selectedAttribute.getType()) {
        case SIMPLE:
          attributeValues.put(selectedAttribute, new ValueDisplay(value));
          break;
        case KEY_AND_DISPLAY:
          String display =
              rowResult
                  .get(AttributeMapping.getDisplayMappingAlias(selectedAttribute.getName()))
                  .getString()
                  .get();
          attributeValues.put(selectedAttribute, new ValueDisplay(value, display));
          break;
        default:
          throw new SystemException("Unknown attribute type: " + selectedAttribute.getType());
      }
    }

    Map<HierarchyField, ValueDisplay> hierarchyFieldValues = new HashMap<>();
    for (HierarchyField selectedHierarchyField : selectedHierarchyFields) {
      CellValue cellValue = rowResult.get(selectedHierarchyField.getHierarchyFieldAlias());
      if (cellValue == null) {
        throw new SystemException(
            "Hierarchy field column not found: "
                + selectedHierarchyField.getHierarchyName()
                + ", "
                + selectedHierarchyField.getType());
      }
      hierarchyFieldValues.put(selectedHierarchyField, new ValueDisplay(cellValue.getLiteral()));
    }

    return new EntityInstance(attributeValues, hierarchyFieldValues);
  }

  public Map<Attribute, ValueDisplay> getAttributeValues() {
    return Collections.unmodifiableMap(attributeValues);
  }

  public Map<HierarchyField, ValueDisplay> getHierarchyFieldValues() {
    return Collections.unmodifiableMap(hierarchyFieldValues);
  }
}
