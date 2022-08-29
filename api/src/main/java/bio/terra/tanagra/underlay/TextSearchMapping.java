package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFTextSearchMapping;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TextSearchMapping {
  private static final String TEXT_SEARCH_COLUMN_ALIAS = "t_text";

  private List<Attribute> attributes;
  private FieldPointer searchString;

  private TextSearchMapping(List<Attribute> attributes) {
    this.attributes = attributes;
  }

  private TextSearchMapping(FieldPointer searchString) {
    this.searchString = searchString;
  }

  public static TextSearchMapping fromSerialized(
      UFTextSearchMapping serialized,
      TablePointer tablePointer,
      Map<String, Attribute> entityAttributes) {
    if (serialized.getAttributes() != null && serialized.getSearchString() != null) {
      throw new IllegalArgumentException(
          "Text search mapping can be defined by either attributes or a search string, not both");
    }

    if (serialized.getAttributes() != null) {
      if (serialized.getAttributes().size() == 0) {
        throw new IllegalArgumentException("Text search mapping list of attributes is empty");
      }
      List<Attribute> attributesForTextSearch =
          serialized.getAttributes().stream()
              .map(a -> entityAttributes.get(a))
              .collect(Collectors.toList());
      return new TextSearchMapping(attributesForTextSearch);
    }

    if (serialized.getSearchString() != null) {
      FieldPointer searchStringField =
          FieldPointer.fromSerialized(serialized.getSearchString(), tablePointer);
      return new TextSearchMapping(searchStringField);
    }

    throw new IllegalArgumentException("Text search mapping is empty");
  }

  public static TextSearchMapping defaultIndexMapping(TablePointer tablePointer) {
    return new TextSearchMapping(new FieldPointer(tablePointer, TEXT_SEARCH_COLUMN_ALIAS));
  }

  public boolean definedByAttributes() {
    return attributes != null;
  }

  public boolean definedBySearchString() {
    return searchString != null;
  }

  public List<Attribute> getAttributes() {
    return Collections.unmodifiableList(attributes);
  }

  public FieldPointer getSearchString() {
    return searchString;
  }
}
