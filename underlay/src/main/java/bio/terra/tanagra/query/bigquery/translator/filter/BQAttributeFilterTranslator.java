package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.AttributeFilter;
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

public class BQAttributeFilterTranslator extends ApiFilterTranslator {
  private final AttributeFilter attributeFilter;

  public BQAttributeFilterTranslator(ApiTranslator apiTranslator, AttributeFilter attributeFilter) {
    super(apiTranslator);
    this.attributeFilter = attributeFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
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
    String whereClause;
    if (attributeFilter.hasUnaryOperator()) {
      whereClause =
          apiTranslator.unaryFilterSql(
              valueField, attributeFilter.getUnaryOperator(), tableAlias, sqlParams);
    } else if (attributeFilter.hasBinaryOperator()) {
      whereClause =
          apiTranslator.binaryFilterSql(
              valueField,
              attributeFilter.getBinaryOperator(),
              attributeFilter.getValues().get(0),
              tableAlias,
              sqlParams);
    } else {
      whereClause =
          apiTranslator.naryFilterSql(
              valueField,
              attributeFilter.getNaryOperator(),
              attributeFilter.getValues(),
              tableAlias,
              sqlParams);
    }

    if (!isSearchOptimized) {
      return whereClause;
    }

    ITEntitySearchByAttribute searchTable =
        attributeFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntitySearchByAttributeTable(entity, attribute);
    SqlQueryField id = SqlQueryField.of(fetchSelectField(searchTable, entity.getIdAttribute()));

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

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.equals(attributeFilter.getAttribute());
  }
}
