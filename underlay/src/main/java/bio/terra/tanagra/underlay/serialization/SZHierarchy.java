package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.Set;

@AnnotatedClass(name = "SZHierarchy", markdown = "Hierarchy for an entity.")
public class SZHierarchy {
  @AnnotatedField(
      name = "SZHierarchy.name",
      markdown =
          "Name of the hierarchy.\n\n"
              + "This is the unique identifier for the hierarchy. In a single entity, the hierarchy names cannot overlap. "
              + "Name may not include spaces or special characters, only letters and numbers. "
              + "The first character must be a letter.\n\n"
              + "If there is only one hierarchy, the name is optional and, if unspecified, will be set to `default`. "
              + "If there are multiple hierarchies, the name is required for each one.",
      optional = true,
      defaultValue = "default")
  public String name;

  @AnnotatedField(
      name = "SZHierarchy.childParentIdPairsSqlFile",
      markdown =
          "Name of the child parent id pairs SQL file.\n\n"
              + "File must be in the same directory as the entity file. Name includes file extension.\n\n"
              + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), "
              + "but the child and parent ids are required.",
      exampleValue = "childParent.sql")
  public String childParentIdPairsSqlFile;

  @AnnotatedField(
      name = "SZHierarchy.childIdFieldName",
      markdown =
          "Name of the field or column name in the [child parent id pairs SQL](${SZHierarchy.childParentIdPairsSqlFile}) "
              + "that maps to the child id.",
      exampleValue = "child")
  public String childIdFieldName;

  @AnnotatedField(
      name = "SZHierarchy.parentIdFieldName",
      markdown =
          "Name of the field or column name in the [child parent id pairs SQL](${SZHierarchy.childParentIdPairsSqlFile}) "
              + "that maps to the parent id.",
      exampleValue = "parent")
  public String parentIdFieldName;

  @AnnotatedField(
      name = "SZHierarchy.rootNodeIds",
      markdown =
          "Set of root ids. Indexing jobs will filter out any hierarchy root nodes that are not in this set. "
              + "If the [root node ids SQL](${SZHierarchy.rootNodeIdsSqlFile}) is defined, then this property "
              + "must be unset.",
      optional = true)
  public Set<Long> rootNodeIds;

  @AnnotatedField(
      name = "SZHierarchy.rootNodeIdsSqlFile",
      markdown =
          "Name of the root id SQL file. File must be in the same directory as the entity file. Name includes file extension.\n\n"
              + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM roots`), but the root id is required. "
              + "Indexing jobs will filter out any hierarchy root nodes that are not returned by this query. "
              + "If the [root node ids set](${SZHierarchy.rootNodeIds}) is defined, then this property must be unset.",
      optional = true,
      exampleValue = "rootNode.sql")
  public String rootNodeIdsSqlFile;

  @AnnotatedField(
      name = "SZHierarchy.rootIdFieldName",
      markdown =
          "Name of the field or column name that maps to the root id.\n\n"
              + "If the [root node ids SQL](${SZHierarchy.rootNodeIdsSqlFile}) is defined, then this property is required. "
              + "If the [root node ids set](${SZHierarchy.rootNodeIds}) is defined, then this property must be unset.",
      optional = true,
      exampleValue = "root_id")
  public String rootIdFieldName;

  @AnnotatedField(
      name = "SZHierarchy.maxDepth",
      markdown =
          "Maximum depth of the hierarchy. If there are branches of the hierarchy that are deeper "
              + "than the number specified here, they will be truncated.")
  public int maxDepth;

  @AnnotatedField(
      name = "SZHierarchy.keepOrphanNodes",
      markdown =
          "An orphan node has no parents or children. "
              + "When false, indexing jobs will filter out orphan nodes. "
              + "When true, indexing jobs skip this filtering step and we keep the orphan nodes in the hierarchy.",
      optional = true,
      defaultValue = "false")
  public boolean keepOrphanNodes;

  @AnnotatedField(
      name = "SZHierarchy.cleanHierarchyNodesWithZeroCounts",
      markdown =
          "When false, indexing jobs will not clean hierarchy nodes with both a zero item and rollup counts. "
              + "When true, indexing jobs will clean hierarchy nodes with both a zero item and rollup counts.",
      optional = true,
      defaultValue = "false")
  public boolean cleanHierarchyNodesWithZeroCounts;
}
