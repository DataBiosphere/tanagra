package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFHierarchyMapping;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class HierarchyMapping {
  private static final String PATH_COLUMN_ALIAS = "t_path";
  private static final String NUM_CHILDREN_COLUMN_ALIAS = "t_numChildren";

  private static final AuxiliaryData CHILD_PARENT_AUXILIARY_DATA =
      new AuxiliaryData("childParent", List.of("child", "parent"));
  private static final AuxiliaryData ROOT_NODES_FILTER_AUXILIARY_DATA =
      new AuxiliaryData("rootNodesFilter", List.of("node"));
  private static final AuxiliaryData ANCESTOR_DESCENDANT_AUXILIARY_DATA =
      new AuxiliaryData("ancestorDescendant", List.of("ancestor", "descendant"));
  private static final AuxiliaryData PATH_NUM_CHILDREN_AUXILIARY_DATA =
      new AuxiliaryData("pathNumChildren", List.of("node", "path", "numChildren"));

  private final AuxiliaryDataMapping childParent;
  private final AuxiliaryDataMapping rootNodesFilter;
  private final AuxiliaryDataMapping ancestorDescendant;
  private final AuxiliaryDataMapping pathNumChildren;

  private HierarchyMapping(
      AuxiliaryDataMapping childParent,
      AuxiliaryDataMapping rootNodesFilter,
      AuxiliaryDataMapping ancestorDescendant,
      AuxiliaryDataMapping pathNumChildren) {
    this.childParent = childParent;
    this.rootNodesFilter = rootNodesFilter;
    this.ancestorDescendant = ancestorDescendant;
    this.pathNumChildren = pathNumChildren;
  }

  public static HierarchyMapping fromSerialized(
      UFHierarchyMapping serialized, String hierarchyName, DataPointer dataPointer) {
    if (serialized.getChildParent() == null) {
      throw new IllegalArgumentException("Child parent pairs are undefined");
    }
    AuxiliaryDataMapping childParent =
        AuxiliaryDataMapping.fromSerialized(
            serialized.getChildParent(), dataPointer, CHILD_PARENT_AUXILIARY_DATA);
    AuxiliaryDataMapping rootNodesFilter =
        serialized.getRootNodesFilter() == null
            ? null
            : AuxiliaryDataMapping.fromSerialized(
                serialized.getRootNodesFilter(), dataPointer, ROOT_NODES_FILTER_AUXILIARY_DATA);
    AuxiliaryDataMapping ancestorDescendant =
        serialized.getAncestorDescendant() == null
            ? null
            : AuxiliaryDataMapping.fromSerialized(
                serialized.getAncestorDescendant(),
                dataPointer,
                ANCESTOR_DESCENDANT_AUXILIARY_DATA);
    AuxiliaryDataMapping pathNumChildren =
        serialized.getPathNumChildren() == null
            ? null
            : AuxiliaryDataMapping.fromSerialized(
                serialized.getPathNumChildren(), dataPointer, PATH_NUM_CHILDREN_AUXILIARY_DATA);
    return new HierarchyMapping(childParent, rootNodesFilter, ancestorDescendant, pathNumChildren);
  }

  public static HierarchyMapping defaultIndexMapping(
      String entityName,
      String hierarchyName,
      TablePointer tablePointer,
      FieldPointer idAttributeField) {
    String tablePrefix = entityName + "_" + hierarchyName + "_";
    DataPointer dataPointer = tablePointer.getDataPointer();

    TablePointer childParentTable =
        TablePointer.fromTableName(
            tablePrefix + CHILD_PARENT_AUXILIARY_DATA.getName(), dataPointer);
    AuxiliaryDataMapping childParent =
        new AuxiliaryDataMapping(
            childParentTable,
            CHILD_PARENT_AUXILIARY_DATA.getFields().stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        fieldName -> new FieldPointer(childParentTable, fieldName))));

    TablePointer ancestorDescendantTable =
        TablePointer.fromTableName(
            tablePrefix + ANCESTOR_DESCENDANT_AUXILIARY_DATA.getName(), dataPointer);
    AuxiliaryDataMapping ancestorDescendant =
        new AuxiliaryDataMapping(
            ancestorDescendantTable,
            ANCESTOR_DESCENDANT_AUXILIARY_DATA.getFields().stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        fieldName -> new FieldPointer(ancestorDescendantTable, fieldName))));

    AuxiliaryDataMapping pathNumChildren =
        new AuxiliaryDataMapping(
            tablePointer,
            Map.of(
                "node",
                idAttributeField,
                "path",
                new FieldPointer(tablePointer, PATH_COLUMN_ALIAS),
                "numChildren",
                new FieldPointer(tablePointer, NUM_CHILDREN_COLUMN_ALIAS)));

    return new HierarchyMapping(childParent, null, ancestorDescendant, pathNumChildren);
  }

  public SQLExpression queryChildParentPairs(String childFieldAlias, String parentFieldAlias) {
    TableVariable childParentTableVar = TableVariable.forPrimary(childParent.getTablePointer());
    FieldVariable childFieldVar =
        new FieldVariable(
            childParent.getFieldPointers().get("child"), childParentTableVar, childFieldAlias);
    FieldVariable parentFieldVar =
        new FieldVariable(
            childParent.getFieldPointers().get("parent"), childParentTableVar, parentFieldAlias);
    return new Query(List.of(childFieldVar, parentFieldVar), List.of(childParentTableVar));
  }

  public SQLExpression queryPossibleRootNodes(String idFieldAlias) {
    TableVariable possibleRootNodesTableVar =
        TableVariable.forPrimary(rootNodesFilter.getTablePointer());
    FieldVariable idFieldVar =
        new FieldVariable(
            rootNodesFilter.getFieldPointers().get("node"),
            possibleRootNodesTableVar,
            idFieldAlias);
    return new Query(List.of(idFieldVar), List.of(possibleRootNodesTableVar));
  }

  public SQLExpression queryAncestorDescendantPairs(
      String ancestorFieldAlias, String descendantFieldAlias) {
    TableVariable ancestorDescendantTableVar =
        TableVariable.forPrimary(ancestorDescendant.getTablePointer());
    FieldVariable ancestorFieldVar =
        new FieldVariable(
            ancestorDescendant.getFieldPointers().get("ancestor"),
            ancestorDescendantTableVar,
            ancestorFieldAlias);
    FieldVariable descendantFieldVar =
        new FieldVariable(
            ancestorDescendant.getFieldPointers().get("descendant"),
            ancestorDescendantTableVar,
            descendantFieldAlias);
    return new Query(
        List.of(ancestorFieldVar, descendantFieldVar), List.of(ancestorDescendantTableVar));
  }

  public AuxiliaryDataMapping getChildParent() {
    return childParent;
  }

  public boolean hasRootNodesFilter() {
    return rootNodesFilter != null;
  }

  public AuxiliaryDataMapping getRootNodesFilter() {
    return rootNodesFilter;
  }

  public boolean hasAncestorDescendant() {
    return ancestorDescendant != null;
  }

  public AuxiliaryDataMapping getAncestorDescendant() {
    return ancestorDescendant;
  }

  public boolean hasPathNumChildren() {
    return pathNumChildren != null;
  }

  public AuxiliaryDataMapping getPathNumChildren() {
    return pathNumChildren;
  }
}
