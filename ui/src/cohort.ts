import { generate } from "randomstring";
import * as tanagra from "./tanagra-api";

export function generateId(): string {
  return generate(8);
}

export interface Cohort {
  id: string;
  name: string;
  underlayName: string;
  attributes: string[];
  groups: Group[];
}

export function generateQueryFilter(
  cohort: Cohort,
  entityVar: string
): tanagra.Filter | null {
  const operands = cohort.groups
    .map((group) => generateFilter(group, entityVar))
    .filter((filter) => filter) as Array<tanagra.Filter>;
  if (operands.length === 0) {
    return null;
  }

  return {
    arrayFilter: {
      operands: operands,
      operator: tanagra.ArrayFilterOperator.And,
    },
  };
}

export enum GroupKind {
  Included = 1,
  Excluded,
}

export interface Group {
  id: string;
  kind: GroupKind;
  criteria: Criteria[];
}

function generateFilter(
  group: Group,
  entityVar: string
): tanagra.Filter | null {
  const operands = group.criteria
    .map((criteria) =>
      getCriteriaPlugin(criteria).generateFilter(entityVar, false)
    )
    .filter((filter) => filter) as Array<tanagra.Filter>;
  if (operands.length === 0) {
    return null;
  }

  return {
    arrayFilter: {
      operands: operands,
      operator: tanagra.ArrayFilterOperator.Or,
    },
  };
}

// Since Redux doesn't allow classes in its data model, criteria specific data
// is stored in an object that the criteria knows the contents of but is
// otherwise opaque except for a set of common methods.
export interface Criteria {
  id: string;
  type: string;
  name: string;
  count: number;
  data: unknown;
}

// CriteriaConfigs are used to initialize CriteriaPlugins and provide a list of
// possible criteria.
export interface CriteriaConfig {
  // The plugin type to use for this criteria.
  type: string;
  title: string;
  defaultName: string;

  // Plugin specific config.
  plugin: unknown;
}

// Having typed data here allows the registry to treat all data as unknown while
// plugins can use an actual type internally.
export interface CriteriaPlugin<DataType> {
  id: string;
  data: DataType;
  renderEdit: (dispatchFn: (data: DataType) => void) => JSX.Element;
  renderDetails: () => JSX.Element;
  generateFilter: (
    entityVar: string,
    fromOccurrence: boolean
  ) => tanagra.Filter | null;
  occurrenceEntities: () => string[];
}

// registerCriteriaPlugin is a decorator that allows criteria to automatically
// register with the app simply by importing them.
export function registerCriteriaPlugin(
  type: string,
  initializeData: (config: CriteriaConfig) => unknown
) {
  return <T extends CriteriaPluginConstructor>(constructor: T): void => {
    criteriaRegistry.set(type, {
      initializeData,
      constructor,
    });
  };
}

export function createCriteria(config: CriteriaConfig): Criteria {
  const entry = getCriteriaEntry(config.type);
  return {
    id: generateId(),
    type: config.type,
    name: config.defaultName,
    count: 0,
    data: entry.initializeData(config),
  };
}

export function getCriteriaPlugin(criteria: Criteria): CriteriaPlugin<unknown> {
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
  new (id: string, data: unknown): CriteriaPlugin<unknown>;
}

type RegistryEntry = {
  initializeData: (config: CriteriaConfig) => unknown;
  constructor: CriteriaPluginConstructor;
};

const criteriaRegistry = new Map<string, RegistryEntry>();
