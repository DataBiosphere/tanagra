package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import java.util.Map;

public class ItemInGroupFilterTranslator extends ApiFilterTranslator {
  private final ItemInGroupFilter itemInGroupFilter;

  public ItemInGroupFilterTranslator(
      ApiTranslator apiTranslator,
      ItemInGroupFilter itemInGroupFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.itemInGroupFilter = itemInGroupFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    GroupItems groupItems = itemInGroupFilter.getGroupItems();
    RelationshipFilter relationshipFilter =
        new RelationshipFilter(
            itemInGroupFilter.getUnderlay(),
            groupItems,
            groupItems.getItemsEntity(),
            groupItems.getGroupItemsRelationship(),
            itemInGroupFilter.getGroupSubFilter(),
            itemInGroupFilter.getGroupByCountAttributes(),
            itemInGroupFilter.getGroupByCountOperator(),
            itemInGroupFilter.getGroupByCountValue());
    return apiTranslator
        .translator(relationshipFilter, attributeSwapFields)
        .buildSql(sqlParams, tableAlias);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}
