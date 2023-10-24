package bio.terra.tanagra.underlay2;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;
import bio.terra.tanagra.underlay2.indexschema.HierarchyAncestorDescendant;
import bio.terra.tanagra.underlay2.indexschema.HierarchyChildParent;
import javax.annotation.Nullable;

public class Hierarchy {
  private final String name;
  private final FieldPointer sourceChildField;
  private final FieldPointer sourceParentField;
  private @Nullable final FieldPointer sourceRootField;
  private final FieldPointer indexChildField;
  private final FieldPointer indexParentField;
  private final FieldPointer indexAncestorField;
  private final FieldPointer indexDescendantField;
  private final FieldPointer indexPathField;
  private final FieldPointer indexNumChildrenField;
  private final int maxDepth;
  private final boolean isKeepOrphanNodes;

  public Hierarchy(
      String entityName,
      String name,
      FieldPointer sourceChildField,
      FieldPointer sourceParentField,
      @Nullable FieldPointer sourceRootField,
      int maxDepth,
      boolean isKeepOrphanNodes) {
    this.name = name;
    this.sourceChildField = sourceChildField;
    this.sourceParentField = sourceParentField;
    this.sourceRootField = sourceRootField;
    this.maxDepth = maxDepth;
    this.isKeepOrphanNodes = isKeepOrphanNodes;

    // Resolve index tables and fields.
    this.indexChildField = HierarchyChildParent.getChildField(entityName, name);
    this.indexParentField = HierarchyChildParent.getParentField(entityName, name);
    this.indexAncestorField = HierarchyAncestorDescendant.getAncestorField(entityName, name);
    this.indexDescendantField = HierarchyAncestorDescendant.getDescendantField(entityName, name);
    this.indexPathField = EntityMain.getHierarchyPathField(entityName, name);
    this.indexNumChildrenField = EntityMain.getHierarchyNumChildrenField(entityName, name);
  }

  public String getName() {
    return name;
  }

  public FieldPointer getSourceChildField() {
    return sourceChildField;
  }

  public FieldPointer getSourceParentField() {
    return sourceParentField;
  }

  public boolean hasRootFilter() {
    return sourceRootField != null;
  }

  public FieldPointer getSourceRootField() {
    if (!hasRootFilter()) {
      throw new SystemException("No root filter defined for hierarchy: " + name);
    }
    return sourceRootField;
  }

  public FieldPointer getIndexChildField() {
    return indexChildField;
  }

  public ColumnSchema getIndexChildColumnSchema() {
    return HierarchyChildParent.Column.CHILD.getSchema();
  }

  public FieldPointer getIndexParentField() {
    return indexParentField;
  }

  public ColumnSchema getIndexParentColumnSchema() {
    return HierarchyChildParent.Column.PARENT.getSchema();
  }

  public FieldPointer getIndexAncestorField() {
    return indexAncestorField;
  }

  public ColumnSchema getIndexAncestorColumnSchema() {
    return HierarchyAncestorDescendant.Column.ANCESTOR.getSchema();
  }

  public FieldPointer getIndexDescendantField() {
    return indexDescendantField;
  }

  public ColumnSchema getIndexDescendantColumnSchema() {
    return HierarchyAncestorDescendant.Column.DESCENDANT.getSchema();
  }

  public FieldPointer getIndexPathField() {
    return indexPathField;
  }

  public FieldPointer getIndexNumChildrenField() {
    return indexNumChildrenField;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public boolean isKeepOrphanNodes() {
    return isKeepOrphanNodes;
  }
}
