package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITEntitySearchByAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BQAttributeFilterTranslator extends ApiFilterTranslator {
  private final List<AttributeFilter> attributeFilters;
  private final LogicalOperator logicalOperator; // not null if List.size > 1

  public BQAttributeFilterTranslator(
      ApiTranslator apiTranslator,
      AttributeFilter singleAttributeFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.attributeFilters = List.of(singleAttributeFilter);
    this.logicalOperator = null;
  }

  private BQAttributeFilterTranslator(
      ApiTranslator apiTranslator,
      List<AttributeFilter> attributeFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.attributeFilters = attributeFilters;
    this.logicalOperator = logicalOperator;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    return attributeFilters.size() == 1
        ? buildSqlForSingleFilter(sqlParams, tableAlias)
        : buildSqlForList(sqlParams, tableAlias);
  }

  private String buildSqlForSingleFilter(SqlParams sqlParams, String tableAlias) {
    AttributeFilter singleFilter = attributeFilters.get(0);
    Entity entity = singleFilter.getEntity();
    ITEntityMain entityTable =
        singleFilter.getUnderlay().getIndexSchema().getEntityMain(entity.getName());

    Attribute attribute = singleFilter.getFilterAttributes().get(0);
    SqlField valueField = fetchSelectField(entityTable, attribute);
    boolean isSearchOptimized =
        entity.containsOptimizeSearchByAttributes(List.of(attribute.getName()));

    // optimized for search on attribute (dataType is not repeated in searchOptimized tables)
    if (!isSearchOptimized && attribute.isDataTypeRepeated()) {
      return buildSqlForRepeatedAttribute(sqlParams, tableAlias, singleFilter, valueField);
    }

    String whereClause = buildWhereSql(singleFilter, sqlParams, tableAlias, valueField);
    return isSearchOptimized
        ? searchOptimizedSql(singleFilter, tableAlias, whereClause)
        : whereClause;
  }

  private String buildSqlForList(SqlParams sqlParams, String tableAlias) {
    // List is used only when all attrs are optimized for search together (private constructor).
    // Attributes are not repeated in searchOptimized tables
    AttributeFilter firstFilter = attributeFilters.get(0);
    ITEntityMain entityTable =
        firstFilter.getUnderlay().getIndexSchema().getEntityMain(firstFilter.getEntity().getName());

    String[] subFilterClauses =
        attributeFilters.stream()
            .map(
                filter ->
                    buildWhereSql(
                        filter,
                        sqlParams,
                        tableAlias,
                        fetchSelectField(entityTable, filter.getFilterAttributes().get(0))))
            .toList()
            .toArray(new String[0]);
    String whereClause = apiTranslator.booleanAndOrFilterSql(logicalOperator, subFilterClauses);
    return searchOptimizedSql(firstFilter, tableAlias, whereClause);
  }

  private String buildSqlForRepeatedAttribute(
      SqlParams sqlParams, String tableAlias, AttributeFilter filter, SqlField valueField) {
    if (filter.hasUnaryOperator()) {
      return apiTranslator.unaryFilterOnRepeatedFieldSql(
          valueField, filter.getUnaryOperator(), tableAlias, sqlParams);
    } else if ((filter.hasBinaryOperator()
            && BinaryOperator.EQUALS.equals(filter.getBinaryOperator()))
        || (filter.hasNaryOperator() && NaryOperator.IN.equals(filter.getNaryOperator()))) {
      return apiTranslator.naryFilterOnRepeatedFieldSql(
          valueField, NaryOperator.IN, filter.getValues(), tableAlias, sqlParams);
    } else if ((filter.hasBinaryOperator()
            && BinaryOperator.NOT_EQUALS.equals(filter.getBinaryOperator()))
        || (filter.hasNaryOperator() && NaryOperator.NOT_IN.equals(filter.getNaryOperator()))) {
      return apiTranslator.naryFilterOnRepeatedFieldSql(
          valueField, NaryOperator.NOT_IN, filter.getValues(), tableAlias, sqlParams);
    }
    throw new InvalidQueryException(
        "Operator not supported for repeated data type attributes: "
            + filter.getOperatorName()
            + ", "
            + filter.getFilterAttributeNames().get(0));
  }

  private String searchOptimizedSql(AttributeFilter filter, String tableAlias, String whereClause) {
    Entity firstEntity = filter.getEntity();
    ITEntitySearchByAttributes searchTable =
        filter
            .getUnderlay()
            .getIndexSchema()
            .getEntitySearchByAttributes(firstEntity, filter.getFilterAttributeNames());
    SqlQueryField id =
        SqlQueryField.of(fetchSelectField(searchTable, firstEntity.getIdAttribute()));
    return id.renderForWhere(tableAlias)
        + " IN ("
        + "SELECT "
        + id.renderForSelect()
        + " FROM "
        + searchTable.getTablePointer().render()
        + " WHERE "
        + whereClause
        + ')';
  }

  private String buildWhereSql(
      AttributeFilter filter, SqlParams sqlParams, String tableAlias, SqlField valueField) {
    // Build sql where clause for the attribute values
    if (filter.hasUnaryOperator()) {
      return apiTranslator.unaryFilterSql(
          valueField, filter.getUnaryOperator(), tableAlias, sqlParams);
    } else if (filter.hasBinaryOperator()) {
      return apiTranslator.binaryFilterSql(
          valueField, filter.getBinaryOperator(), filter.getValues().get(0), tableAlias, sqlParams);
    } else {
      return apiTranslator.naryFilterSql(
          valueField, filter.getNaryOperator(), filter.getValues(), tableAlias, sqlParams);
    }
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attributeFilters.size() == 1
        && attribute.equals(attributeFilters.get(0).getFilterAttributes().get(0));
  }

  public static Optional<ApiFilterTranslator> mergedTranslator(
      ApiTranslator apiTranslator,
      List<AttributeFilter> attributeFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    Entity firstEntity = attributeFilters.get(0).getEntity();
    List<String> allFilterAttributeNames =
        attributeFilters.stream().flatMap(l -> l.getFilterAttributeNames().stream()).toList();

    // All attrs must be optimized for search together.
    // Otherwise, the filters may already run on the same table, no change needed
    return firstEntity.containsOptimizeSearchByAttributes(allFilterAttributeNames)
        ? Optional.of(
            new BQAttributeFilterTranslator(
                apiTranslator, attributeFilters, logicalOperator, attributeSwapFields))
        : Optional.empty();
  }
}
