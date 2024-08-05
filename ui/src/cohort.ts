import { ROLLUP_COUNT_ATTRIBUTE, SortDirection } from "data/configuration";
import { MergedItem, mergeLists } from "data/mergeLists";
import {
  Cohort,
  CommonSelectorConfig,
  Criteria,
  FeatureSet,
  Group,
  GroupSection,
  GroupSectionFilterKind,
  GroupSectionReducingOperator,
  UnderlaySource,
} from "data/source";
import { DataEntry } from "data/types";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useStudyId, useUnderlay } from "hooks";
import { generate } from "randomstring";
import { ReactNode } from "react";
import useSWRImmutable from "swr/immutable";
import { isValid } from "util/valid";

export function generateId(): string {
  return generate(8);
}

export function sectionName(section: GroupSection, index: number) {
  return section.name || "group " + String(index + 1);
}

const DEFAULT_SECTION_ID = "_default";

function newSectionInternal(
  id: string,
  criteria?: Criteria,
  group?: Group
): GroupSection {
  const groups: Group[] = [];
  if (criteria) {
    groups.push(defaultGroup(criteria));
  } else if (group) {
    groups.push(group);
  }

  return {
    id,
    filter: {
      kind: GroupSectionFilterKind.Any,
      excluded: false,
    },
    groups,
    secondBlockGroups: [],
    operatorValue: 3,
    firstBlockReducingOperator: GroupSectionReducingOperator.Any,
    secondBlockReducingOperator: GroupSectionReducingOperator.Any,
  };
}

export function newSection(
  criteria?: Criteria,
  id?: string,
  group?: Group
): GroupSection {
  return newSectionInternal(id ?? generateId(), criteria, group);
}

export function defaultSection(): GroupSection {
  return newSectionInternal(DEFAULT_SECTION_ID);
}

export function defaultGroup(criteria: Criteria): Group {
  return {
    id: criteria.id,
    // TODO: **************** Is this necessary?
    entity: "",
    criteria: [criteria],
  };
}

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
  return (
    (criteria.predefinedDisplayName ?? criteria.config.displayName) +
    (title.length > 0 ? `: ${title}` : "")
  );
}

export function getCriteriaTitleFull<DataType>(
  criteria: Criteria,
  plugin?: CriteriaPlugin<DataType>
) {
  const p = plugin ?? getCriteriaPlugin(criteria);
  const details = p.displayDetails();
  const title = details.additionalText?.join(", ") || details.title;
  return (
    (criteria.predefinedDisplayName ?? criteria.config.displayName) +
    (title.length > 0 ? `: ${title}` : "")
  );
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
      SortDirection.Desc,
      (value: DataEntry) => value[ROLLUP_COUNT_ATTRIBUTE]
    ),
  }));
}

export type OccurrenceFilters = {
  id: string;
  name: string;
  attributes: string[];
  sourceCriteria: string[];
  sql?: string;
};

export function useOccurrenceList(
  cohorts: Cohort[],
  featureSets: FeatureSet[],
  includeAllAttributes?: boolean
) {
  const underlay = useUnderlay();
  const underlaySource = useUnderlaySource();
  const studyId = useStudyId();

  return useSWRImmutable(
    {
      type: "occurrenceFilters",
      cohorts,
      featureSets,
    },
    async () => {
      const previewEntities = await underlaySource.exportPreviewEntities(
        underlay.name,
        studyId,
        cohorts.map((c) => c.id),
        featureSets.map((fs) => fs.id),
        includeAllAttributes
      );

      return previewEntities.map((pe) => {
        const sourceCriteria = pe.sourceCriteria.map((sc) => {
          const featureSet = featureSets.find(
            (fs) => fs.id === sc.conceptSetId
          );
          if (!featureSet) {
            throw new Error(
              `Unexpected source feature set: ${sc.conceptSetId}`
            );
          }

          let criteria: Criteria | undefined;
          if (featureSet.predefinedCriteria.includes(sc.criteriaId)) {
            criteria = underlaySource.createPredefinedCriteria(sc.criteriaId);
          } else {
            criteria = featureSet?.criteria?.find(
              (c) => c.id === sc.criteriaId
            );
          }
          if (!criteria) {
            throw new Error(
              `Unexpected source criteria: feature set: ${sc.conceptSetId}, criteria: ${sc.criteriaId}`
            );
          }

          return getCriteriaTitle(criteria);
        });

        return {
          id: pe.id,
          name: pe.name,
          attributes: pe.attributes,
          sourceCriteria,
          sql: pe.sql,
        };
      });
    },
    { keepPreviousData: true }
  );
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
