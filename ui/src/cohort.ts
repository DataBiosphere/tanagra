import {
  Filter,
  FilterType,
  makeArrayFilter,
  UnaryFilterOperator,
} from "data/filter";
import { MergedDataEntry, Source } from "data/source";
import { DataEntry } from "data/types";
import { generate } from "randomstring";
import { ReactNode } from "react";
import * as tanagra from "tanagra-api";
import { isValid } from "util/valid";
import { CriteriaConfig } from "./underlaysSlice";

export function generateId(): string {
  return generate(8);
}

export function generateCohortFilter(cohort: tanagra.Cohort): Filter | null {
  return makeArrayFilter(
    {},
    cohort.groupSections
      .map((section) => generateSectionFilter(section))
      .filter(isValid)
  );
}

function generateSectionFilter(section: tanagra.GroupSection): Filter | null {
  const filter = makeArrayFilter(
    section.filter.kind === tanagra.GroupSectionFilterKindEnum.Any
      ? { min: 1 }
      : {},
    section.groups
      .map((group) => generateGroupSectionFilter(group))
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

function generateGroupSectionFilter(group: tanagra.Group): Filter | null {
  const plugins = group.criteria.map((c) => getCriteriaPlugin(c));
  const filter = makeArrayFilter(
    {},
    plugins.map((p) => p.generateFilter()).filter(isValid)
  );

  if (!filter || !group.entity) {
    return filter;
  }

  const groupByCountFilters = plugins
    .map((p) => p.groupByCountFilter?.())
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
    entityId: group.entity,
    subfilter: filter,
    groupByCount:
      groupByCountFilters.length > 0 ? groupByCountFilters[0] : undefined,
  };
}

export function sectionName(section: tanagra.GroupSection, index: number) {
  return section.name || "Group " + String(index + 1);
}

export function defaultSection(
  criteria?: tanagra.Criteria
): tanagra.GroupSection {
  return {
    id: generateId(),
    filter: {
      kind: tanagra.GroupSectionFilterKindEnum.Any,
      excluded: false,
    },
    groups: !!criteria ? [defaultGroup(criteria)] : [],
  };
}

export function defaultGroup(criteria: tanagra.Criteria): tanagra.Group {
  return {
    id: generateId(),
    entity: getCriteriaPlugin(criteria).filterOccurrenceId(),
    criteria: [criteria],
  };
}

export const defaultFilter: tanagra.GroupSectionFilter = {
  kind: tanagra.GroupSectionFilterKindEnum.Any,
  excluded: false,
};

// Having typed data here allows the registry to treat all data generically
// while plugins can use an actual type internally.
export interface CriteriaPlugin<DataType> {
  id: string;
  data: DataType;
  renderEdit?: (
    doneAction: () => void,
    setBackURL: (url?: string) => void
  ) => JSX.Element;
  renderInline: (groupId: string) => ReactNode;
  displayDetails: () => DisplayDetails;
  generateFilter: () => Filter | null;
  groupByCountFilter?: () => tanagra.GroupByCount | null;
  filterOccurrenceId: () => string;
  outputOccurrenceIds?: () => string[];
}

export type DisplayDetails = {
  title: string;
  standaloneTitle?: boolean;
  additionalText?: string[];
};

export function getCriteriaTitle<DataType>(
  criteria: tanagra.Criteria,
  plugin?: CriteriaPlugin<DataType>
) {
  const p = plugin ?? getCriteriaPlugin(criteria);
  const title = p.displayDetails().title;
  return criteria.config.title + (title.length > 0 ? `: ${title}` : "");
}

export function searchCriteria(
  source: Source,
  configs: CriteriaConfig[],
  query: string
): Promise<SearchResponse> {
  const promises: Promise<[string, DataEntry[]]>[] = configs
    .map((config) => {
      const entry = criteriaRegistry.get(config.type);
      if (!entry?.search) {
        return null;
      }

      // The compiler can't seem to understand this expression if it's not
      // explicitly typed.
      const p: Promise<[string, DataEntry[]]> = entry
        .search(source, config, query)
        .then((res) => [config.id, res]);
      return p;
    })
    .filter(isValid);

  return Promise.all(promises).then((responses) => ({
    data: source.mergeDataEntryLists(responses, 100),
  }));
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
  source: Source,
  config: CriteriaConfig,
  dataEntry?: DataEntry
) => object;

type SearchFn = (
  source: Source,
  config: CriteriaConfig,
  query: string
) => Promise<DataEntry[]>;

export type SearchResponse = {
  data: MergedDataEntry[];
};

export function createCriteria(
  source: Source,
  config: CriteriaConfig,
  dataEntry?: DataEntry
): tanagra.Criteria {
  const entry = getCriteriaEntry(config.type);
  return {
    id: generateId(),
    type: config.type,
    data: entry.initializeData(source, config, dataEntry),
    config: config,
  };
}

export function getCriteriaPlugin(
  criteria: tanagra.Criteria,
  entity?: string
): CriteriaPlugin<object> {
  return new (getCriteriaEntry(criteria.type).constructor)(
    criteria.id,
    criteria.config as CriteriaConfig,
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
    config: CriteriaConfig,
    data: object,
    entity?: string
  ): CriteriaPlugin<object>;
}

type RegistryEntry = {
  initializeData: InitializeDataFn;
  constructor: CriteriaPluginConstructor;
  search?: SearchFn;
};

const criteriaRegistry = new Map<string, RegistryEntry>();
