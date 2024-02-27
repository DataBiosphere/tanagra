package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;

public class GroupHasItemsFilterTranslator extends ApiFilterTranslator {
  private final GroupHasItemsFilter groupHasItemsFilter;

  public GroupHasItemsFilterTranslator(
      ApiTranslator apiTranslator, GroupHasItemsFilter groupHasItemsFilter) {
    super(apiTranslator);
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
            groupHasItemsFilter.getGroupByCountAttribute(),
            groupHasItemsFilter.getGroupByCountOperator(),
            groupHasItemsFilter.getGroupByCountValue());
    return apiTranslator.translator(relationshipFilter).buildSql(sqlParams, tableAlias);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
