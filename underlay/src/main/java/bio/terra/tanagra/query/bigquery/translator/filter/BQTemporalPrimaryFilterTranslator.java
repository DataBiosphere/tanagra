package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.TemporalPrimaryFilter;
import bio.terra.tanagra.api.shared.JoinOperator;
import bio.terra.tanagra.api.shared.ReducingOperator;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;

import javax.annotation.Nullable;
import java.util.List;

public class BQTemporalPrimaryFilterTranslator extends ApiFilterTranslator {
    private static final String PRIMARY_ENTITY_ID_ALIAS = "primary_entity_id";
    private final TemporalPrimaryFilter temporalPrimaryFilter;

    public BQTemporalPrimaryFilterTranslator(
            ApiTranslator apiTranslator, TemporalPrimaryFilter temporalPrimaryFilter) {
        super(apiTranslator);
        this.temporalPrimaryFilter = temporalPrimaryFilter;
    }

    @Override
    public String buildSql(SqlParams sqlParams, String tableAlias) {
        // Build the temp table queries for the first and second conditions.
        String firstConditionQuery = buildQueryForCondition(temporalPrimaryFilter.getFirstCondition(), temporalPrimaryFilter.getFirstConditionQualifier());
        String secondConditionQuery = buildQueryForCondition(temporalPrimaryFilter.getSecondCondition(), temporalPrimaryFilter.getSecondConditionQualifier());

        // Join the temp tables for the first and second conditions.
        String joinQuery = buildQueryJoiningConditions(
                List.of(SqlField.of(PRIMARY_ENTITY_ID_ALIAS)),
                firstConditionQuery, secondConditionQuery,
                temporalPrimaryFilter.getJoinOperator(),
                temporalPrimaryFilter.getJoinOperatorValue());

        // Wrap in a WHERE clause.
        // [WHERE] primaryEntity.id IN (join query)
        ITEntityMain selectEntityTable =
                temporalPrimaryFilter
                        .getUnderlay()
                        .getIndexSchema()
                        .getEntityMain(temporalPrimaryFilter.getUnderlay().getPrimaryEntity().getName());
        Attribute selectIdAttribute = temporalPrimaryFilter.getUnderlay().getPrimaryEntity().getIdAttribute();
        SqlField selectIdField =
                attributeSwapFields.containsKey(selectIdAttribute)
                        ? attributeSwapFields.get(selectIdAttribute)
                        : selectEntityTable.getAttributeValueField(selectIdAttribute.getName());
        return SqlQueryField.of(selectIdField).renderForWhere(tableAlias) + " IN (" + joinQuery + ')';
    }

    private static String buildQueryForCondition(List<EntityOutput> entityOutputs, @Nullable ReducingOperator reducingOperator) {
        // Set the included attributes:
        //   - Foreign key attribute to primary entity
        //   - Temporal date attribute
        //   - Temporal visit attribute

        // SELECT includedAttributes,
        //   RANK() OVER (PARTITION BY primaryEntityId ORDER BY date ASC) AS orderRank
        // FROM (... UNION ALL ...)

        // Wrap to filter on rank.
        // SELECT * FROM (above query) WHERE orderRank=1
        return "";
    }

    private static String buildQueryJoiningConditions(
            List<SqlField> selectFields, String firstConditionQuery, String secondConditionQuery,
            JoinOperator joinOperator, @Nullable Integer joinOperatorValue) {
        // Join the temp tables for the first and second conditions.

        // SELECT firstCondition.primaryEntityId
        // FROM (temp table query 1) AS firstCondition
        // JOIN (temp table query 2) AS secondCondition
        //   ON secondCondition.primaryEntityId = firstCondition.primaryEntityId
        //   [AND secondCondition.visitId = firstCondition.visitId]
        //   [AND secondCondition.date >= firstCondition + operatorValue]
        return "";
    }

    @Override
    public boolean isFilterOnAttribute(Attribute attribute) { return attribute.isId(); }
}
