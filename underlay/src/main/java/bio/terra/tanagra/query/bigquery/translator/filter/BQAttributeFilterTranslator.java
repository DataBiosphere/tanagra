package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.shared.*;
import bio.terra.tanagra.exception.*;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITEntitySearchByAttribute;
import java.util.List;

public class BQAttributeFilterTranslator extends ApiFilterTranslator {
  private final AttributeFilter attributeFilter;
  private final List<AttributeFilter> attributeFilterList;
  private final LogicalOperator logicalOperatorForList;

  public BQAttributeFilterTranslator(
      ApiTranslator apiTranslator, AttributeFilter singleAttributeFilter) {
    super(apiTranslator);
    this.attributeFilter = singleAttributeFilter;
    this.attributeFilterList = null;
    this.logicalOperatorForList = null;
  }

  public BQAttributeFilterTranslator(
      ApiTranslator apiTranslator,
      List<AttributeFilter> attributeFilters,
      LogicalOperator logicalOperator) {
    super(apiTranslator);
    this.attributeFilter = null;
    this.attributeFilterList = attributeFilters;
    this.logicalOperatorForList = logicalOperator;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    if (attributeFilter != null) {
      return buildSqlForSingleFilter(sqlParams, tableAlias);
    } else {
      return buildSqlForList(sqlParams, tableAlias);
    }
  }

  private String buildSqlForSingleFilter(SqlParams sqlParams, String tableAlias) {
    Entity entity = attributeFilter.getEntity();
    ITEntityMain entityTable =
        attributeFilter.getUnderlay().getIndexSchema().getEntityMain(entity.getName());
    Attribute attribute = attributeFilter.getAttribute();
    SqlField valueField = fetchSelectField(entityTable, attribute);

    // search attribute-specific table if attribute is optimized for search
    boolean isSearchOptimized = entity.containsOptimizeSearchByAttribute(attribute.getName());

    if (!isSearchOptimized && attribute.isDataTypeRepeated()) {
      boolean naryOperatorIn =
          (attributeFilter.hasBinaryOperator()
                  && BinaryOperator.EQUALS.equals(attributeFilter.getBinaryOperator()))
              || (attributeFilter.hasNaryOperator()
                  && NaryOperator.IN.equals(attributeFilter.getNaryOperator()));
      boolean naryOperatorNotIn =
          (attributeFilter.hasBinaryOperator()
                  && BinaryOperator.NOT_EQUALS.equals(attributeFilter.getBinaryOperator()))
              || (attributeFilter.hasNaryOperator()
                  && NaryOperator.NOT_IN.equals(attributeFilter.getNaryOperator()));
      if (!naryOperatorIn && !naryOperatorNotIn) {
        throw new InvalidQueryException(
            "Operator not supported for repeated data type attributes: "
                + attributeFilter.getOperatorName()
                + ", "
                + attribute.getName());
      }
      return apiTranslator.naryFilterOnRepeatedFieldSql(
          valueField,
          naryOperatorIn ? NaryOperator.IN : NaryOperator.NOT_IN,
          attributeFilter.getValues(),
          tableAlias,
          sqlParams);
    }

    // Build sql where clause for the attribute values
    String whereClause = buildWhereSql(attributeFilter, sqlParams, tableAlias, valueField);
    return isSearchOptimized
        ? searchOptimizedSql(attributeFilter, tableAlias, whereClause)
        : whereClause;
  }

  private String buildSqlForList(SqlParams sqlParams, String tableAlias) {
    AttributeFilter firstFilter = attributeFilterList.get(0);
    Entity entity = firstFilter.getEntity();
    ITEntityMain entityTable =
        firstFilter.getUnderlay().getIndexSchema().getEntityMain(entity.getName());

    String[] subFilterClauses =
        attributeFilterList.stream()
            .map(
                filter ->
                    buildWhereSql(
                        filter,
                        sqlParams,
                        tableAlias,
                        fetchSelectField(entityTable, filter.getAttribute())))
            .toList()
            .toArray(new String[0]);

    String whereClause =
        apiTranslator.booleanAndOrFilterSql(logicalOperatorForList, subFilterClauses);

    return searchOptimizedSql(firstFilter, tableAlias, whereClause);
  }

  private String searchOptimizedSql(AttributeFilter filter, String tableAlias, String whereClause) {
    Entity firstEntity = filter.getEntity();
    ITEntitySearchByAttribute searchTable =
        filter
            .getUnderlay()
            .getIndexSchema()
            .getEntitySearchByAttributeTable(firstEntity, filter.getAttribute());
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
    return attributeFilter != null && attribute.equals(attributeFilter.getAttribute());
  }

  public static boolean canMergeTranslation(List<AttributeFilter> attributeFilters) {
    // Can merge (AND) the 'where' clauses if are all optimized on search together
    AttributeFilter firstFilter = attributeFilters.get(0);
    Entity firstEntity = firstFilter.getEntity();
    Attribute firstAttribute = firstFilter.getAttribute();

    if (!firstEntity.containsOptimizeSearchByAttribute(firstAttribute.getName())) {
      // first attribute itself is not optimized for search
      return false;
    }

    List<String> searchTableAttributes =
        firstFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntitySearchByAttributeTable(firstEntity, firstAttribute)
            .getAttributeNames();

    // check if all attributes in the filters are in the same search table for the same entity
    return attributeFilters.stream()
        .allMatch(
            filter ->
                filter.getEntity().getName().equals(firstEntity.getName())
                    && searchTableAttributes.contains(filter.getAttribute().getName()));
  }
}
