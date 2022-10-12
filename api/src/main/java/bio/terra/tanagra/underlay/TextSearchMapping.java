package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.UnionQuery;
import bio.terra.tanagra.serialization.UFTextSearchMapping;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TextSearchMapping {
  private static final String TEXT_SEARCH_ID_COLUMN_NAME = "id";
  private static final String TEXT_SEARCH_STRING_COLUMN_NAME = "text";
  private static final AuxiliaryData TEXT_SEARCH_STRING_AUXILIARY_DATA =
      new AuxiliaryData(
          "textsearch", List.of(TEXT_SEARCH_ID_COLUMN_NAME, TEXT_SEARCH_STRING_COLUMN_NAME));

  private List<Attribute> attributes;
  private FieldPointer searchString;
  private AuxiliaryDataMapping searchStringTable;
  private final Attribute idAttribute;

  private TextSearchMapping(List<Attribute> attributes, Attribute idAttribute) {
    this.attributes = attributes;
    this.idAttribute = idAttribute;
  }

  private TextSearchMapping(FieldPointer searchString, Attribute idAttribute) {
    this.searchString = searchString;
    this.idAttribute = idAttribute;
  }

  private TextSearchMapping(AuxiliaryDataMapping searchStringTable, Attribute idAttribute) {
    this.searchStringTable = searchStringTable;
    this.idAttribute = idAttribute;
  }

  public static TextSearchMapping fromSerialized(
      UFTextSearchMapping serialized,
      TablePointer tablePointer,
      Map<String, Attribute> entityAttributes,
      String idAttributeName) {
    if (serialized.getAttributes() != null && serialized.getSearchString() != null) {
      throw new InvalidConfigException(
          "Text search mapping can be defined by either attributes or a search string, not both");
    }

    Attribute idAttribute = entityAttributes.get(idAttributeName);
    if (serialized.getAttributes() != null) {
      if (serialized.getAttributes().size() == 0) {
        throw new InvalidConfigException("Text search mapping list of attributes is empty");
      }
      List<Attribute> attributesForTextSearch =
          serialized.getAttributes().stream()
              .map(a -> entityAttributes.get(a))
              .collect(Collectors.toList());
      return new TextSearchMapping(attributesForTextSearch, idAttribute);
    }

    if (serialized.getSearchString() != null) {
      FieldPointer searchStringField =
          FieldPointer.fromSerialized(serialized.getSearchString(), tablePointer);
      return new TextSearchMapping(searchStringField, idAttribute);
    }

    if (serialized.getSearchStringTable() != null) {
      AuxiliaryDataMapping searchStringTable =
          AuxiliaryDataMapping.fromSerialized(
              serialized.getSearchStringTable(),
              tablePointer.getDataPointer(),
              TEXT_SEARCH_STRING_AUXILIARY_DATA);
      return new TextSearchMapping(searchStringTable, idAttribute);
    }

    throw new InvalidConfigException("Text search mapping is empty");
  }

  public static TextSearchMapping defaultIndexMapping(
      String entityName, TablePointer tablePointer, Attribute idAttribute) {
    // TODO: Change default index mapping to be a field in the same table as the denormalized entity
    // instances.
    String tablePrefix = entityName + "_";
    TablePointer idTextStringTable =
        TablePointer.fromTableName(
            tablePrefix + TEXT_SEARCH_STRING_AUXILIARY_DATA.getName(),
            tablePointer.getDataPointer());

    return new TextSearchMapping(
        new FieldPointer.Builder()
            .tablePointer(tablePointer)
            .columnName(idAttribute.getName())
            .foreignTablePointer(idTextStringTable)
            .foreignKeyColumnName(TEXT_SEARCH_ID_COLUMN_NAME)
            .foreignColumnName(TEXT_SEARCH_STRING_COLUMN_NAME)
            .build(),
        idAttribute);
  }

  public Query queryTextSearchStrings(EntityMapping entityMapping) {
    SQLExpression idAllTextPairs;
    if (definedByAttributes()) {
      // TODO: Allow specifying non-STRING attributes, but wrap them in CAST to STRING.
      getAttributes()
          .forEach(
              attr -> {
                if (!attr.getDataType().equals(Literal.DataType.STRING)) {
                  throw new InvalidConfigException(
                      "All text search attributes must have datatype STRING: "
                          + attr.getName()
                          + ","
                          + attr.getDataType());
                }
              });
      idAllTextPairs =
          new UnionQuery(
              getAttributes().stream()
                  .map(
                      attr ->
                          entityMapping.queryAttributes(
                              Map.of(
                                  TEXT_SEARCH_ID_COLUMN_NAME,
                                  idAttribute,
                                  TEXT_SEARCH_STRING_COLUMN_NAME,
                                  attr)))
                  .collect(Collectors.toList()));
    } else if (definedBySearchString()) {
      idAllTextPairs =
          entityMapping.queryAttributesAndFields(
              Map.of(TEXT_SEARCH_ID_COLUMN_NAME, idAttribute),
              Map.of(TEXT_SEARCH_STRING_COLUMN_NAME, getSearchString()));
    } else if (definedBySearchStringAuxiliaryData()) {
      TableVariable searchStringTableVar =
          TableVariable.forPrimary(searchStringTable.getTablePointer());
      List<TableVariable> tableVars = List.of(searchStringTableVar);
      FieldVariable idFieldVar =
          searchStringTable
              .getFieldPointers()
              .get(TEXT_SEARCH_ID_COLUMN_NAME)
              .buildVariable(searchStringTableVar, tableVars);
      FieldVariable textFieldVar =
          searchStringTable
              .getFieldPointers()
              .get(TEXT_SEARCH_STRING_COLUMN_NAME)
              .buildVariable(searchStringTableVar, tableVars);
      idAllTextPairs =
          new Query.Builder().select(List.of(idFieldVar, textFieldVar)).tables(tableVars).build();
    } else {
      throw new SystemException("Unknown text search mapping type");
    }

    TablePointer idTextPairsTable =
        TablePointer.fromRawSql(
            idAllTextPairs.renderSQL(), entityMapping.getTablePointer().getDataPointer());
    FieldPointer idField =
        new FieldPointer.Builder()
            .tablePointer(idTextPairsTable)
            .columnName(TEXT_SEARCH_ID_COLUMN_NAME)
            .build();
    FieldPointer concatenatedTextField =
        new FieldPointer.Builder()
            .tablePointer(idTextPairsTable)
            .columnName(TEXT_SEARCH_STRING_COLUMN_NAME)
            .sqlFunctionWrapper("STRING_AGG")
            .build();

    TableVariable idTextPairsTableVar = TableVariable.forPrimary(idTextPairsTable);
    FieldVariable idFieldVar =
        new FieldVariable(idField, idTextPairsTableVar, TEXT_SEARCH_ID_COLUMN_NAME);
    FieldVariable concatenatedTextFieldVar =
        new FieldVariable(
            concatenatedTextField, idTextPairsTableVar, TEXT_SEARCH_STRING_COLUMN_NAME);
    return new Query.Builder()
        .select(List.of(idFieldVar, concatenatedTextFieldVar))
        .tables(List.of(idTextPairsTableVar))
        .groupBy(List.of(idFieldVar))
        .build();
  }

  public TablePointer getTablePointer(EntityMapping entityMapping) {
    if (definedByAttributes()) {
      return entityMapping.getTablePointer();
    } else if (definedBySearchString()) {
      return searchString.getForeignTablePointer();
    } else if (definedBySearchStringAuxiliaryData()) {
      return searchStringTable.getTablePointer();
    } else {
      throw new SystemException("Unknown text search mapping type");
    }
  }

  public boolean definedByAttributes() {
    return attributes != null;
  }

  public boolean definedBySearchString() {
    return searchString != null;
  }

  public boolean definedBySearchStringAuxiliaryData() {
    return searchStringTable != null;
  }

  public List<Attribute> getAttributes() {
    return Collections.unmodifiableList(attributes);
  }

  public FieldPointer getSearchString() {
    return searchString;
  }

  public AuxiliaryDataMapping getSearchStringTable() {
    return searchStringTable;
  }
}
