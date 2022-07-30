package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFTextSearchMapping;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TextSearchMapping {
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
    if (serialized.attributes != null && serialized.searchString != null) {
      throw new IllegalArgumentException(
          "Text search mapping can be defined by either attributes or a search string, not both");
    }

    if (serialized.attributes != null) {
      if (serialized.attributes.size() == 0) {
        throw new IllegalArgumentException("Text search mapping list of attributes is empty");
      }
      List<Attribute> attributesForTextSearch =
          serialized.attributes.stream()
              .map(a -> entityAttributes.get(a))
              .collect(Collectors.toList());
      return new TextSearchMapping(attributesForTextSearch);
    }

    if (serialized.searchString != null) {
      FieldPointer searchStringField =
          FieldPointer.fromSerialized(serialized.searchString, tablePointer);
      return new TextSearchMapping(searchStringField);
    }

    throw new IllegalArgumentException("Text search mapping is empty");
  }
}
