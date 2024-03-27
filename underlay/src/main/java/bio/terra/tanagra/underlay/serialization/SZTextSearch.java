package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import java.util.Set;

@AnnotatedClass(name = "SZTextSearch", markdown = "Text search configuration for an entity.")
public class SZTextSearch {

  @AnnotatedField(
      name = "SZTextSearch.attributes",
      markdown =
          "Set of attributes to allow text search on. Text search on attributes not included here is unsupported.",
      optional = true)
  public Set<String> attributes;

  @AnnotatedField(
      name = "SZTextSearch.idTextPairsSqlFile",
      markdown =
          "Name of the id text pairs SQL file. "
              + "File must be in the same directory as the entity file. Name includes file extension.\n\n"
              + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM synonyms`), "
              + "but the entity id and text string is required. The SQL query may return multiple rows per entity id.",
      optional = true,
      exampleValue = "textSearch.sql")
  public String idTextPairsSqlFile;

  @AnnotatedField(
      name = "SZTextSearch.idFieldName",
      markdown =
          "Name of the field or column name that maps to the entity id. "
              + "If the [id text pairs SQL](${SZTextSearch.idTextPairsSqlFile}) is defined, then this property is required.",
      optional = true,
      exampleValue = "id")
  public String idFieldName;

  @AnnotatedField(
      name = "SZTextSearch.textFieldName",
      markdown =
          "Name of the field or column name that maps to the text search string. "
              + "If the [id text pairs SQL](${SZTextSearch.idTextPairsSqlFile}) is defined, then this property is required.",
      optional = true,
      exampleValue = "text")
  public String textFieldName;
}
