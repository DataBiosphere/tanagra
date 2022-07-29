package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFTextSearchMapping;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TextSearchMapping {
  private List<Attribute> attributes;
  private FieldPointer searchStringField;

  private TextSearchMapping(List<Attribute> attributes) {
    this.attributes = attributes;
  }

  private TextSearchMapping(FieldPointer searchStringField) {
    this.searchStringField = searchStringField;
  }

  public static TextSearchMapping fromSerialized(
      UFTextSearchMapping serialized,
      TablePointer tablePointer,
      Map<String, Attribute> entityAttributes) {
    if (serialized.attributes != null && serialized.searchStringField != null) {
      throw new IllegalArgumentException(
          "Text search mapping can be defined by either attributes or a search string field, not both");
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

    if (serialized.searchStringField != null) {
      FieldPointer searchStringField =
          FieldPointer.fromSerialized(serialized.searchStringField, tablePointer);
      return new TextSearchMapping(searchStringField);
    }

    throw new IllegalArgumentException("Text search mapping is empty");
  }
}
