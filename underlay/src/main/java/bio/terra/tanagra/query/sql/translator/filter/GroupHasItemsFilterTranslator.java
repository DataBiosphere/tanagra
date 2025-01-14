package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import java.util.Map;

public class GroupHasItemsFilterTranslator extends ApiFilterTranslator {
  private final GroupHasItemsFilter groupHasItemsFilter;

  public GroupHasItemsFilterTranslator(
      ApiTranslator apiTranslator,
      GroupHasItemsFilter groupHasItemsFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.groupHasItemsFilter = groupHasItemsFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    GroupItems groupItems = groupHasItemsFilter.getGroupItems();
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            groupHasItemsFilter.getUnderlay(),
            groupItems,
            groupItems.getGroupEntity(),
            groupItems.getGroupItemsRelationship(),
            groupHasItemsFilter.getItemsSubFilter(),
            groupHasItemsFilter.getGroupByCountAttributes(),
            groupHasItemsFilter.getGroupByCountOperator(),
            groupHasItemsFilter.getGroupByCountValue());
    return apiTranslator
        .translator(relationshipFilter, attributeSwapFields)
        .buildSql(sqlParams, tableAlias);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
