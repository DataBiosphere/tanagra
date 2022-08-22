import {
  Filter,
  FilterType,
  makeArrayFilter,
  UnaryFilterOperator,
} from "data/filter";
import { Source } from "data/source";
import { generate } from "randomstring";
import * as tanagra from "tanagra-api";
import { isValid } from "util/valid";
import { CriteriaConfig } from "./underlaysSlice";

export function generateId(): string {
  return generate(8);
}

export function generateCohortFilter(cohort: tanagra.Cohort): Filter | null {
  return makeArrayFilter(
    {},
    cohort.groups.map((group) => generateFilter(group)).filter(isValid)
  );
}

function generateFilter(group: tanagra.Group): Filter | null {
  const filter = makeArrayFilter(
    { min: 1 },
    group.criteria
      .map((criteria) => getCriteriaPlugin(criteria).generateFilter())
      .filter(isValid)
  );

  if (!filter || group.kind === tanagra.GroupKindEnum.Included) {
    return filter;
  }
  return {
    type: FilterType.Unary,
    operator: UnaryFilterOperator.Not,
    operand: filter,
  };
}

// Having typed data here allows the registry to treat all data generically
// while plugins can use an actual type internally.
export interface CriteriaPlugin<DataType> {
  id: string;
  data: DataType;
  renderEdit: (dispatchFn: (data: DataType) => void) => JSX.Element;
  renderDetails: () => JSX.Element;
  generateFilter: () => Filter | null;
  occurrenceID: () => string;
}

// registerCriteriaPlugin is a decorator that allows criteria to automatically
// register with the app simply by importing them.
export function registerCriteriaPlugin(
  type: string,
  initializeData: (source: Source, config: CriteriaConfig) => object
) {
  return <T extends CriteriaPluginConstructor>(constructor: T): void => {
    criteriaRegistry.set(type, {
      initializeData,
      constructor,
    });
  };
}

export function createCriteria(
  source: Source,
  config: CriteriaConfig
): tanagra.Criteria {
  const entry = getCriteriaEntry(config.type);
  return {
    id: generateId(),
    type: config.type,
    name: config.defaultName,
    data: entry.initializeData(source, config),
  };
}

export function getCriteriaPlugin(
  criteria: tanagra.Criteria
): CriteriaPlugin<object> {
  return new (getCriteriaEntry(criteria.type).constructor)(
    criteria.id,
    criteria.data
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
  new (id: string, data: object): CriteriaPlugin<object>;
}

type RegistryEntry = {
  initializeData: (source: Source, config: CriteriaConfig) => object;
  constructor: CriteriaPluginConstructor;
};

const criteriaRegistry = new Map<string, RegistryEntry>();
