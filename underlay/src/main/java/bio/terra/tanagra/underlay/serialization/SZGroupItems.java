package bio.terra.tanagra.underlay.serialization;

/**
 * Group-Items entity group configuration.
 *
 * <p>Define a version of this file for each entity group of this type.
 *
 * <p>This entity group type defines a one-to-many relationship between two entities. For each group
 * entity instance, there are one or more items entity instances.
 */
public class SZGroupItems {
  /**
   * Name of the entity group.
   *
   * <p>This is the unique identifier for the entity group. In a single underlay, the entity group
   * names of any group type cannot overlap.
   *
   * <p>Name may not include spaces or special characters, only letters and numbers. The first
   * character must be a letter.
   */
  public String name;

  /** Name of the group entity. */
  public String groupEntity;

  /** Name of the items entity. */
  public String itemsEntity;

  /**
   * <strong>(optional)</strong> Attribute of the items entity that is a foreign key to the id
   * attribute of the group entity.
   *
   * <p>If this property is set, then the {@link #idPairsSqlFile} must be unset.
   */
  public String foreignKeyAttributeItemsEntity;

  /**
   * <strong>(optional)</strong> Name of the group entity - items entity id pairs SQL file.
   *
   * <p>File must be in the same directory as the entity group file. Name includes file extension
   * (e.g. idPairs.sql).
   *
   * <p>There can be other columns selected in the SQL file (e.g. <code>SELECT * FROM relationships
   * </code>), but the group and items entity ids are required.
   *
   * <p>If this property is set, then the {@link #foreignKeyAttributeItemsEntity} must be unset.
   */
  public String idPairsSqlFile;

  /**
   * <strong>(optional)</strong> Name of the field or column name that maps to the group entity id.
   *
   * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.
   */
  public String groupEntityIdFieldName;

  /**
   * <strong>(optional)</strong> Name of the field or column name that maps to the items entity id.
   *
   * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.
   */
  public String itemsEntityIdFieldName;
}
