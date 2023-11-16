package bio.terra.tanagra.underlay.serialization;

/**
 * <p>Group-Items entity group configuration.</p>
 * <p>Define a version of this file for each entity group of this type.</p>
 * <p>This entity group type defines a one-to-many relationship between two entities.
 * For each group entity instance, there are one or more items entity instances.</p>
 */
public class SZGroupItems {
  /**
   * <p>Name of the entity group.</p>
   * <p>This is the unique identifier for the entity group.
   * In a single underlay, the entity group names of any group type cannot overlap.</p>
   * <p>Name may not include spaces or special characters, only letters and numbers.
   * The first character must be a letter.</p>
   */
  public String name;

  /**
   * <p>Name of the group entity.</p>
   */
  public String groupEntity;

  /**
   * <p>Name of the items entity.</p>
   */
  public String itemsEntity;

  /**
   * <p><strong>(optional)</strong> Attribute of the items entity that is a foreign key to the id attribute of the group entity.</p>
   * <p>If this property is set, then the {@link #idPairsSqlFile} must be unset.</p>
   */
  public String foreignKeyAttributeItemsEntity;

  /**
   * <p><strong>(optional)</strong> Name of the group entity - items entity id pairs SQL file.</p>
   * <p>File must be in the same directory as the entity group file. Name includes file extension (e.g. idPairs.sql).</p>
   * <p>There can be other columns selected in the SQL file (e.g. <code>SELECT * FROM relationships</code>),
   * but the group and items entity ids are required.</p>
   * <p>If this property is set, then the {@link #foreignKeyAttributeItemsEntity} must be unset.</p>
   */
  public String idPairsSqlFile;

  /**
   * <p><strong>(optional)</strong> Name of the field or column name that maps to the group entity id.</p>
   * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.</p>
   */
  public String groupEntityIdFieldName;

  /**
   * <p><strong>(optional)</strong> Name of the field or column name that maps to the items entity id.</p>
   * <p>If the {@link #idPairsSqlFile} property is defined, then this property is required.</p>
   */
  public String itemsEntityIdFieldName;
}
