package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFHierarchyMapping;
import java.util.List;

public final class HierarchyMapping {
  private static final String ID_FIELD_NAME = "id";
  public static final String CHILD_FIELD_NAME = "child";
  public static final String PARENT_FIELD_NAME = "parent";
  public static final String ANCESTOR_FIELD_NAME = "ancestor";
  public static final String DESCENDANT_FIELD_NAME = "descendant";
  public static final String PATH_FIELD_NAME = "path";
  public static final String NUM_CHILDREN_FIELD_NAME = "num_children";
  public static final String IS_ROOT_FIELD_NAME = "is_root";
  public static final String IS_MEMBER_FIELD_NAME = "is_member";
  private static final AuxiliaryData CHILD_PARENT_AUXILIARY_DATA =
      new AuxiliaryData("childParent", List.of(CHILD_FIELD_NAME, PARENT_FIELD_NAME));
  private static final AuxiliaryData ROOT_NODES_FILTER_AUXILIARY_DATA =
      new AuxiliaryData("rootNodesFilter", List.of(ID_FIELD_NAME));
  private static final AuxiliaryData ANCESTOR_DESCENDANT_AUXILIARY_DATA =
      new AuxiliaryData("ancestorDescendant", List.of(ANCESTOR_FIELD_NAME, DESCENDANT_FIELD_NAME));
  private static final AuxiliaryData PATH_NUM_CHILDREN_AUXILIARY_DATA =
      new AuxiliaryData(
          "pathNumChildren", List.of(ID_FIELD_NAME, PATH_FIELD_NAME, NUM_CHILDREN_FIELD_NAME));

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
      throw new InvalidConfigException("Child parent pairs are undefined");
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
      String entityName, String hierarchyName, TablePointer tablePointer) {
    String tablePrefix = entityName + "_" + hierarchyName + "_";
    DataPointer dataPointer = tablePointer.getDataPointer();

    AuxiliaryDataMapping childParent =
        AuxiliaryDataMapping.defaultIndexMapping(
            CHILD_PARENT_AUXILIARY_DATA, tablePrefix, dataPointer);
    AuxiliaryDataMapping ancestorDescendant =
        AuxiliaryDataMapping.defaultIndexMapping(
            ANCESTOR_DESCENDANT_AUXILIARY_DATA, tablePrefix, dataPointer);
    AuxiliaryDataMapping pathNumChildren =
        AuxiliaryDataMapping.defaultIndexMapping(
            PATH_NUM_CHILDREN_AUXILIARY_DATA, tablePrefix, dataPointer);

    return new HierarchyMapping(childParent, null, ancestorDescendant, pathNumChildren);
  }

  public SQLExpression queryChildParentPairs(String childFieldAlias, String parentFieldAlias) {
    TableVariable childParentTableVar = TableVariable.forPrimary(childParent.getTablePointer());
    FieldVariable childFieldVar =
        new FieldVariable(
            childParent.getFieldPointers().get(CHILD_FIELD_NAME),
            childParentTableVar,
            childFieldAlias);
    FieldVariable parentFieldVar =
        new FieldVariable(
            childParent.getFieldPointers().get(PARENT_FIELD_NAME),
            childParentTableVar,
            parentFieldAlias);
    return new Query.Builder()
        .select(List.of(childFieldVar, parentFieldVar))
        .tables(List.of(childParentTableVar))
        .build();
  }

  public SQLExpression queryPossibleRootNodes(String idFieldAlias) {
    TableVariable possibleRootNodesTableVar =
        TableVariable.forPrimary(rootNodesFilter.getTablePointer());
    FieldVariable idFieldVar =
        new FieldVariable(
            rootNodesFilter.getFieldPointers().get(ID_FIELD_NAME),
            possibleRootNodesTableVar,
            idFieldAlias);
    return new Query.Builder()
        .select(List.of(idFieldVar))
        .tables(List.of(possibleRootNodesTableVar))
        .build();
  }

  public SQLExpression queryAncestorDescendantPairs(
      String ancestorFieldAlias, String descendantFieldAlias) {
    TableVariable ancestorDescendantTableVar =
        TableVariable.forPrimary(ancestorDescendant.getTablePointer());
    FieldVariable ancestorFieldVar =
        new FieldVariable(
            ancestorDescendant.getFieldPointers().get(ANCESTOR_FIELD_NAME),
            ancestorDescendantTableVar,
            ancestorFieldAlias);
    FieldVariable descendantFieldVar =
        new FieldVariable(
            ancestorDescendant.getFieldPointers().get(DESCENDANT_FIELD_NAME),
            ancestorDescendantTableVar,
            descendantFieldAlias);
    return new Query.Builder()
        .select(List.of(ancestorFieldVar, descendantFieldVar))
        .tables(List.of(ancestorDescendantTableVar))
        .build();
  }

  /** Build a field pointer to the PATH or NUM_CHILDREN field, foreign key'd off the entity ID. */
  public FieldPointer buildPathNumChildrenFieldPointerFromEntityId(
      FieldPointer entityIdFieldPointer, String fieldName) {
    FieldPointer fieldInAuxTable = pathNumChildren.getFieldPointers().get(fieldName);
    FieldPointer idFieldInAuxTable = pathNumChildren.getFieldPointers().get(ID_FIELD_NAME);

    // TODO: Handle the case where the path field is in the same table (i.e. not FK'd).
    return new FieldPointer.Builder()
        .tablePointer(entityIdFieldPointer.getTablePointer())
        .columnName(entityIdFieldPointer.getColumnName())
        .foreignTablePointer(pathNumChildren.getTablePointer())
        .foreignKeyColumnName(idFieldInAuxTable.getColumnName())
        .foreignColumnName(fieldInAuxTable.getColumnName())
        .build();
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
