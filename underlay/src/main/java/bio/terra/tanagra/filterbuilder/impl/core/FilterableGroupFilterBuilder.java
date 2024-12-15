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
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.AttributeSchemaUtils;
import bio.terra.tanagra.filterbuilder.impl.core.utils.EntityGroupFilterUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFFilterableGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTFilterableGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                      buildForIndividualSelectAll(
                          config, underlay, groupItems, selectionItem.getAll()));
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
      CFFilterableGroup.FilterableGroup config,
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
      subFiltersToAnd.addAll(
          generateFilterForQuery(config, underlay, nonPrimaryEntity, selectAllItem.getQuery()));
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

  private List<EntityFilter> generateFilterForQuery(
      CFFilterableGroup.FilterableGroup config, Underlay underlay, Entity entity, String query) {
    if (StringUtils.isEmpty(query)) {
      return List.of();
    }

    if (config.getSearchConfigsList().isEmpty()) {
      return List.of(
          new TextSearchFilter(underlay, entity, TextSearchOperator.EXACT_MATCH, query, null));
    }

    List<EntityFilter> filters = new ArrayList<>();
    for (CFFilterableGroup.FilterableGroup.SearchConfig searchConfig :
        config.getSearchConfigsList()) {
      Pattern pattern = Pattern.compile(searchConfig.getRegex());
      Matcher matcher = pattern.matcher(query);
      if (!matcher.matches()) {
        continue;
      }

      for (int i = 0; i < searchConfig.getParametersList().size(); i++) {
        CFFilterableGroup.FilterableGroup.SearchConfig.Parameter param =
            searchConfig.getParameters(i);
        Attribute attribute = entity.getAttribute(param.getAttribute());

        String group = matcher.groupCount() > 0 ? matcher.group(i + 1) : matcher.group(0);
        if (param.getCase()
            == CFFilterableGroup.FilterableGroup.SearchConfig.Parameter.Case.CASE_LOWER) {
          group = group.toLowerCase();
        } else if (param.getCase()
            == CFFilterableGroup.FilterableGroup.SearchConfig.Parameter.Case.CASE_UPPER) {
          group = group.toUpperCase();
        }

        Literal literal = Literal.forGenericString(attribute.getDataType(), group);
        filters.add(
            new AttributeFilter(
                underlay, entity, attribute, lookupOperator(param.getOperator()), literal));
      }
      break;
    }

    return filters;
  }

  private static BinaryOperator lookupOperator(
      CFFilterableGroup.FilterableGroup.SearchConfig.Parameter.Operator operator) {
    return switch (operator) {
      case OPERATOR_EQUALS -> BinaryOperator.EQUALS;
      case OPERATOR_GREATER_THAN -> BinaryOperator.GREATER_THAN;
      case OPERATOR_GREATER_THAN_OR_EQUAL -> BinaryOperator.GREATER_THAN_OR_EQUAL;
      case OPERATOR_LESS_THAN -> BinaryOperator.LESS_THAN;
      case OPERATOR_LESS_THAN_OR_EQUAL -> BinaryOperator.LESS_THAN_OR_EQUAL;
      default -> throw new InvalidQueryException("Unhandled operator type: " + operator);
    };
  }
}
