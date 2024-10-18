package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.EntityGroupFilterUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFFilterableGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTFilterableGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.LoggerFactory;

public class FilterableGroupFilterBuilder
    extends FilterBuilder<CFFilterableGroup.FilterableGroup, DTFilterableGroup.FilterableGroup> {
  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(FilterableGroupFilterBuilder.class);

  public FilterableGroupFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() != 1) {
      throw new InvalidQueryException(
          "Filterable group filter builder does not support modifiers.");
    }

    DTFilterableGroup.FilterableGroup data = deserializeData(selectionData.get(0).getPluginData());
    if (data == null) { // Empty selection data = null filter for a cohort.
      return null;
    }

    // Pull the entity group from the config.
    CFFilterableGroup.FilterableGroup config = deserializeConfig();
    EntityGroup entityGroup = underlay.getEntityGroup(config.getEntityGroup());
    if (entityGroup.getType() != EntityGroup.Type.GROUP_ITEMS) {
      throw new InvalidQueryException(
          "Filterable group filter builder only supports entityGroup of type GROUP_ITEMS.");
    }
    GroupItems groupItems = (GroupItems) entityGroup;

    // Apply Boolean.OR to individual selection
    List<EntityFilter> filtersToOr = new ArrayList<>();

    // We want to build one filter per selection item, not one filter per selected id.
    List<Literal> selectedIds = new ArrayList<>();
    data.getSelectedList()
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
    if (!selectedIds.isEmpty()) {
      filtersToOr.add(
          EntityGroupFilterUtils.buildGroupItemsFilterFromIds(
              underlay, criteriaSelector, groupItems, selectedIds, List.of()));
    }

    // Grouping is not needed since filtersToOr are not subFilters
    return new BooleanAndOrFilter(LogicalOperator.OR, filtersToOr);
  }

  private EntityFilter buildForIndividualSelectAll(
      Underlay underlay,
      GroupItems groupItems,
      DTFilterableGroup.FilterableGroup.SelectAll selectAllItem) {
    // selectAll: Apply Boolean.AND to individual selections
    List<EntityFilter> filtersToAnd = new ArrayList<>();

    // string query
    if (!selectAllItem.getQuery().isEmpty()) {
      if (selectAllItem.getAttribute().isEmpty()) {
        throw new InvalidQueryException(
            "SelectAllItem query must also include the attribute to query on.");
      }
      // TODO-Dex: to be implemented
    }

    // attribute values
    filtersToAnd.add(
        EntityGroupFilterUtils.buildGroupItemsFilterFromValueData(
            underlay, criteriaSelector, groupItems, selectAllItem.getValueDataList(), List.of()));

    // exclusions: NOT(ids)
    List<Literal> excludedIds =
        selectAllItem.getExclusionsList().stream()
            .filter(item -> item.hasKey() && item.getKey().hasInt64Key())
            .map(item -> Literal.forInt64(item.getKey().getInt64Key()))
            .toList();
    filtersToAnd.add(
        new BooleanNotFilter(
            EntityGroupFilterUtils.buildGroupItemsFilterFromIds(
                underlay, criteriaSelector, groupItems, excludedIds, List.of())));

    // Grouping is not needed since filtersToAnd are not subFilters
    return new BooleanAndOrFilter(LogicalOperator.AND, filtersToAnd);
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
}
