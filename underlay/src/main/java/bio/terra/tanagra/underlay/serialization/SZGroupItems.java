package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZGroupItems",
    markdown =
        "Group-Items entity group configuration.\n\n"
            + "Define a version of this file for each entity group of this type. "
            + "This entity group type defines a one-to-many relationship between two entities. "
            + "For each group entity instance, there are one or more items entity instances.")
public class SZGroupItems {
  @AnnotatedField(
      name = "SZGroupItems.name",
      markdown =
          "Name of the entity group.\n\n"
              + "This is the unique identifier for the entity group. In a single underlay, the entity group "
              + "names of any group type cannot overlap. Name may not include spaces or special characters, "
              + "only letters and numbers. The first character must be a letter.")
  public String name;

  @AnnotatedField(name = "SZGroupItems.groupEntity", markdown = "Name of the group entity.")
  public String groupEntity;

  @AnnotatedField(name = "SZGroupItems.itemsEntity", markdown = "Name of the items entity.")
  public String itemsEntity;

  @AnnotatedField(
      name = "SZGroupItems.foreignKeyAttributeItemsEntity",
      markdown =
          "Attribute of the items entity that is a foreign key to the id attribute of the group entity.\n\n"
              + "If this property is set, then the [id pairs SQL](${SZGroupItems.idPairsSqlFile}) must be unset.",
      optional = true)
  public String foreignKeyAttributeItemsEntity;

  @AnnotatedField(
      name = "SZGroupItems.idPairsSqlFile",
      markdown =
          "Name of the group entity - items entity id pairs SQL file.\n\n"
              + "If this property is set, then the [id pairs SQL](${SZGroupItems.idPairsSqlFile}) must be unset. "
              + "File must be in the same directory as the entity group file. Name includes file extension.\n\n"
              + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the "
              + "group and items entity ids are required. If this property is set, then the "
              + "[foreign key atttribute](${SZGroupItems.foreignKeyAttributeItemsEntity}) must be unset.",
      optional = true,
      exampleValue = "idPairs.sql")
  public String idPairsSqlFile;

  @AnnotatedField(
      name = "SZGroupItems.groupEntityIdFieldName",
      markdown =
          "Name of the field or column name that maps to the group entity id.\n\n"
              + "Required if the [id pairs SQL](${SZGroupItems.idPairsSqlFile}) is defined.",
      optional = true,
      exampleValue = "group_id")
  public String groupEntityIdFieldName;

  @AnnotatedField(
      name = "SZGroupItems.itemsEntityIdFieldName",
      markdown =
          "Name of the field or column name that maps to the items entity id.\n\n"
              + "Required if the [id pairs SQL](${SZGroupItems.idPairsSqlFile}) is defined.",
      optional = true,
      exampleValue = "items_id")
  public String itemsEntityIdFieldName;
}
