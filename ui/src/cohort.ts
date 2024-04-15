import { ROLLUP_COUNT_ATTRIBUTE, SortDirection } from "data/configuration";
import {
  Filter,
  FilterType,
  makeArrayFilter,
  UnaryFilterOperator,
} from "data/filter";
import { MergedItem, mergeLists } from "data/mergeLists";
import {
  Cohort,
  CommonSelectorConfig,
  Criteria,
  Group,
  GroupSection,
  GroupSectionFilter,
  GroupSectionFilterKind,
  UnderlaySource,
} from "data/source";
import { DataEntry, GroupByCount } from "data/types";
import { generate } from "randomstring";
import { ReactNode } from "react";
import { isValid } from "util/valid";

export function generateId(): string {
  return generate(8);
}

export function generateCohortFilter(
  underlaySource: UnderlaySource,
  cohort: Cohort
): Filter | null {
  return makeArrayFilter(
    {},
    cohort.groupSections
      .map((section) => generateSectionFilter(underlaySource, section))
      .filter(isValid)
  );
}

function generateSectionFilter(
  underlaySource: UnderlaySource,
  section: GroupSection
): Filter | null {
  const filter = makeArrayFilter(
    section.filter.kind === GroupSectionFilterKind.Any ? { min: 1 } : {},
    section.groups
      .map((group) => generateGroupSectionFilter(underlaySource, group))
      .filter(isValid)
  );

  if (!filter || !section.filter.excluded) {
    return filter;
  }
  return {
    type: FilterType.Unary,
    operator: UnaryFilterOperator.Not,
    operand: filter,
  };
}

function generateGroupSectionFilter(
  underlaySource: UnderlaySource,
  group: Group
): Filter | null {
  const plugins = group.criteria.map((c) => getCriteriaPlugin(c));

  // There should always be a primary criteria.
  if (!plugins.length) {
    return null;
  }

  // For a person to be selected, the criteria can match any related occurrence.
  const entityFilters = plugins[0]
    .filterEntityIds(underlaySource)
    .map((entity) => {
      const filter = makeArrayFilter(
        {},
        plugins
          .map((p) => p.generateFilter(entity, underlaySource))
          .filter(isValid)
      );

      if (!filter || !entity) {
        return filter;
      }

      const groupByCountFilters = plugins
        .map((p) => p.groupByCountFilter?.(entity))
        .filter(isValid);
      if (groupByCountFilters.length > 1) {
        throw new Error(
          `Criteria groups may not have multiple group by count filters: ${JSON.stringify(
            groupByCountFilters
          )}`
        );
      }

      return {
        type: FilterType.Relationship,
        entityId: entity,
        subfilter: filter,
        groupByCount:
          groupByCountFilters.length > 0 ? groupByCountFilters[0] : undefined,
      };
    });
  return makeArrayFilter({ min: 1 }, entityFilters);
}

export function sectionName(section: GroupSection, index: number) {
  return section.name || "group " + String(index + 1);
}

export function defaultSection(criteria?: Criteria): GroupSection {
  return {
    id: generateId(),
    filter: {
      kind: GroupSectionFilterKind.Any,
      excluded: false,
    },
    groups: !!criteria ? [defaultGroup(criteria)] : [],
  };
}

export function defaultGroup(criteria: Criteria): Group {
  return {
    id: criteria.id,
    // TODO: **************** Is this necessary?
    entity: "",
    criteria: [criteria],
  };
}

export const defaultFilter: GroupSectionFilter = {
  kind: GroupSectionFilterKind.Any,
  excluded: false,
};

// Having typed data here allows the registry to treat all data generically
// while plugins can use an actual type internally.
export interface CriteriaPlugin<DataType> {
  id: string;
  data: DataType;
  renderEdit?: (
    doneAction: () => void,
    setBackAction: (action?: () => void) => void
  ) => JSX.Element;
  renderInline: (groupId: string) => ReactNode;
  displayDetails: () => DisplayDetails;
  generateFilter: (
    occurrenceId: string,
    underlaySource: UnderlaySource
  ) => Filter | null;
  groupByCountFilter?: (occurrenceId: string) => GroupByCount | null;
  filterEntityIds: (underlaySource: UnderlaySource) => string[];
  outputEntityIds?: () => string[];
}

export type DisplayDetails = {
  title: string;
  standaloneTitle?: boolean;
  additionalText?: string[];
};

export function getCriteriaTitle<DataType>(
  criteria: Criteria,
  plugin?: CriteriaPlugin<DataType>
) {
  const p = plugin ?? getCriteriaPlugin(criteria);
  const title = p.displayDetails().title;
  return criteria.config.displayName + (title.length > 0 ? `: ${title}` : "");
}

export function getCriteriaTitleFull<DataType>(
  criteria: Criteria,
  plugin?: CriteriaPlugin<DataType>
) {
  const p = plugin ?? getCriteriaPlugin(criteria);
  const details = p.displayDetails();
  const title = details.additionalText?.join(", ") || details.title;
  return criteria.config.displayName + (title.length > 0 ? `: ${title}` : "");
}

export function searchCriteria(
  underlaySource: UnderlaySource,
  configs: CommonSelectorConfig[],
  query: string
): Promise<SearchResponse> {
  const promises: Promise<[string, DataEntry[]]>[] = configs
    .map((config) => {
      const entry = criteriaRegistry.get(config.plugin);
      if (!entry?.search) {
        return null;
      }

      // The compiler can't seem to understand this expression if it's not
      // explicitly typed.
      const p: Promise<[string, DataEntry[]]> = entry
        .search(underlaySource, config, query)
        .then((res) => [config.name, res]);
      return p;
    })
    .filter(isValid);

  return Promise.all(promises).then((responses) => ({
    data: mergeLists(
      responses,
      100,
      SortDirection.Asc,
      (value: DataEntry) => value[ROLLUP_COUNT_ATTRIBUTE]
    ),
  }));
}

export type OccurrenceFilters = {
  id: string;
  name: string;
  attributes: string[];
  filters: Filter[];
  sourceCriteria: string[];
};

export function getOccurrenceList(
  underlaySource: UnderlaySource,
  selectedCriteria: Set<string>,
  userCriteria?: Criteria[]
): OccurrenceFilters[] {
  const occurrences = new Map<string, Filter[]>();
  const sourceCriteria: string[] = [];
  const addFilter = (occurrence: string, filter?: Filter | null) => {
    if (!occurrences.has(occurrence)) {
      occurrences.set(occurrence, []);
    }
    if (filter) {
      occurrences.get(occurrence)?.push(filter);
    }
  };

  const pc = underlaySource.underlay.prepackagedDataFeatures
    ?.filter((criteria) => selectedCriteria.has(criteria.name))
    ?.map((criteria) => underlaySource.createPredefinedCriteria(criteria.name));

  [...pc, ...(userCriteria ?? [])]
    ?.filter((criteria) => selectedCriteria.has(criteria.id))
    ?.forEach((criteria) => {
      const plugin = getCriteriaPlugin(criteria);
      sourceCriteria.push(getCriteriaTitle(criteria, plugin));

      const occurrenceIds =
        plugin.outputEntityIds?.() ?? plugin.filterEntityIds(underlaySource);
      occurrenceIds.forEach((o) => {
        addFilter(o, plugin.generateFilter(o, underlaySource));
      });
    });

  return Array.from(occurrences)
    .sort()
    .map(([id, filters]) => {
      const entity = underlaySource.lookupEntity(id);
      return {
        id,
        name: entity.displayName ?? entity.name,
        attributes: underlaySource.listAttributes(id),
        filters,
        sourceCriteria,
      };
    });
}

// registerCriteriaPlugin is a decorator that allows criteria to automatically
// register with the app simply by importing them.
export function registerCriteriaPlugin(
  type: string,
  initializeData: InitializeDataFn,
  search?: SearchFn
) {
  return <T extends CriteriaPluginConstructor>(constructor: T): void => {
    criteriaRegistry.set(type, {
      initializeData,
      constructor,
      search,
    });
  };
}

type InitializeDataFn = (
  underlaySource: UnderlaySource,
  config: CommonSelectorConfig,
  dataEntry?: DataEntry
) => string;

type SearchFn = (
  underlaySource: UnderlaySource,
  config: CommonSelectorConfig,
  query: string
) => Promise<DataEntry[]>;

export type SearchResponse = {
  data: MergedItem<DataEntry>[];
};

export function createCriteria(
  underlaySource: UnderlaySource,
  config: CommonSelectorConfig,
  dataEntry?: DataEntry
): Criteria {
  const entry = getCriteriaEntry(config.plugin);
  return {
    id: generateId(),
    type: config.plugin,
    data: entry.initializeData(underlaySource, config, dataEntry),
    config: config,
  };
}

export function getCriteriaPlugin(
  criteria: Criteria,
  entity?: string
): CriteriaPlugin<string> {
  return new (getCriteriaEntry(criteria.type).constructor)(
    criteria.id,
    criteria.config,
    criteria.data,
    entity
  );
}

function getCriteriaEntry(type: string): RegistryEntry {
  const entry = criteriaRegistry.get(type);
  if (!entry) {
    throw `Unknown criteria plugin type '${type}'`;
  }
  return entry;
}

interface CriteriaPluginConstructor {
  new (
    id: string,
    config: CommonSelectorConfig,
    data: string,
    entity?: string
  ): CriteriaPlugin<string>;
}

type RegistryEntry = {
  initializeData: InitializeDataFn;
  constructor: CriteriaPluginConstructor;
  search?: SearchFn;
};

const criteriaRegistry = new Map<string, RegistryEntry>();
