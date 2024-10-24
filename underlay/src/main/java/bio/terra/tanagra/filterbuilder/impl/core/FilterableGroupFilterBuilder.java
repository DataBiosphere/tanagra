package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter.TextSearchOperator;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.AttributeSchemaUtils;
import bio.terra.tanagra.filterbuilder.impl.core.utils.EntityGroupFilterUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFFilterableGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTFilterableGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

public class FilterableGroupFilterBuilder
    extends FilterBuilder<CFFilterableGroup.FilterableGroup, DTFilterableGroup.FilterableGroup> {

  public FilterableGroupFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() != 1) {
      throw new InvalidQueryException(
          "Filterable group filter builder does not support modifiers.");
    }

    // Pull the entity group from the config.
    CFFilterableGroup.FilterableGroup config = deserializeConfig();
    EntityGroup entityGroup = underlay.getEntityGroup(config.getEntityGroup());
    if (entityGroup.getType() != EntityGroup.Type.GROUP_ITEMS) {
      throw new InvalidConfigException(
          "Filterable group filter builder only supports entityGroup of type GROUP_ITEMS.");
    }
    GroupItems groupItems = (GroupItems) entityGroup;

    // Apply Boolean.OR to individual selection
    List<EntityFilter> filtersToOr = new ArrayList<>();

    // We want to build one filter per selection item, not one filter per selected id.
    List<Literal> selectedIds = new ArrayList<>();
    deserializeData(selectionData.get(0).getPluginData())
        .getSelectedList()
        .forEach(
            selectionItem -> {
              switch (selectionItem.getSelectionCase()) {
                case SINGLE:
                  DTFilterableGroup.FilterableGroup.SingleSelect item = selectionItem.getSingle();
                  if (item.hasKey() && item.getKey().hasInt64Key()) {
                    selectedIds.add(Literal.forInt64(item.getKey().getInt64Key()));
                  }
                  break;

                case ALL:
                  filtersToOr.add(
                      buildForIndividualSelectAll(underlay, groupItems, selectionItem.getAll()));
                  break;

                default:
                  throw new InvalidQueryException(
                      "SelectionItem must be either a single selection or selectAll.");
              }
            });

    // singleSelect: search on primary key of the entity
    // filtersToOr.empty: no selection data - return participants with any variant data
    if (!selectedIds.isEmpty() || filtersToOr.isEmpty()) {
      filtersToOr.add(
          EntityGroupFilterUtils.buildGroupItemsFilterFromIds(
              underlay, criteriaSelector, groupItems, selectedIds, List.of()));
    }

    // Grouping is not needed since filtersToOr are not subFilters
    return new BooleanAndOrFilter(
        LogicalOperator.OR, filtersToOr.stream().filter(Objects::nonNull).toList());
  }

  private EntityFilter buildForIndividualSelectAll(
      Underlay underlay,
      GroupItems groupItems,
      DTFilterableGroup.FilterableGroup.SelectAll selectAllItem) {
    // selectAll: Apply Boolean.AND to individual selections
    List<EntityFilter> subFiltersToAnd = new ArrayList<>();

    Entity nonPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // string query
    if (!selectAllItem.getQuery().isEmpty()) {
      subFiltersToAnd.add(
          generateFilterForQuery(underlay, nonPrimaryEntity, selectAllItem.getQuery()));
    }

    // attribute values
    selectAllItem
        .getValueDataList()
        .forEach(
            valueData ->
                subFiltersToAnd.add(
                    AttributeSchemaUtils.buildForEntity(
                        underlay,
                        nonPrimaryEntity,
                        nonPrimaryEntity.getAttribute(valueData.getAttribute()),
                        valueData)));

    // exclusions: NOT(ids)
    List<Literal> excludedIds =
        selectAllItem.getExclusionsList().stream()
            .filter(item -> item.hasKey() && item.getKey().hasInt64Key())
            .map(item -> Literal.forInt64(item.getKey().getInt64Key()))
            .toList();
    if (!excludedIds.isEmpty()) {
      subFiltersToAnd.add(
          EntityGroupFilterUtils.buildIdSubFilter(underlay, nonPrimaryEntity, excludedIds, true));
    }

    // Grouping is not needed since filtersToAnd are not subFilters
    return EntityGroupFilterUtils.buildGroupItemsFilterFromSubFilters(
        underlay, criteriaSelector, groupItems, subFiltersToAnd, List.of());
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    // TODO(BENCH-4362): Add support for data features in filterableGroup filter
    throw new NotImplementedException(
        "Filterable group filter builder does not implemented for data feature set.");
  }

  @Override
  public CFFilterableGroup.FilterableGroup deserializeConfig() {
    return deserializeFromJsonOrProtoBytes(
            criteriaSelector.getPluginConfig(), CFFilterableGroup.FilterableGroup.newBuilder())
        .build();
  }

  @Override
  public DTFilterableGroup.FilterableGroup deserializeData(String serialized) {
    return (serialized == null || serialized.isEmpty())
        ? null
        : deserializeFromJsonOrProtoBytes(
                serialized, DTFilterableGroup.FilterableGroup.newBuilder())
            .build();
  }

  private static EntityFilter generateFilterForQuery(
      Underlay underlay, Entity entity, String query) {
    if (StringUtils.isEmpty(query)) {
      return null;
    }

    // TODO(BENCH-4370): Make this part of underlay config
    if (!entity.getName().equals("variant")) {
      return null;
    }
    Literal literal = Literal.forString(query);
    if (query.matches("rs[0-9]+")) {
      return new AttributeFilter(
          underlay, entity, entity.getAttribute("rs_number"), NaryOperator.IN, List.of(literal));
    } else if (query.matches("[0-9]+-[0-9]+-[A-Z]+-[A-Z]+")) {
      return new AttributeFilter(
          underlay, entity, entity.getAttribute("variant_id"), BinaryOperator.EQUALS, literal);
    } else {
      return new TextSearchFilter(
          underlay, entity, TextSearchOperator.EXACT_MATCH, query, entity.getAttribute("gene"));
    }
  }
}
