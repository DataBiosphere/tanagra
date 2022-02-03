import { generate } from "randomstring";
import * as tanagra from "./tanagra-api";

export function generateId(): string {
  return generate(8);
}

export interface Cohort {
  id: string;
  name: string;
  underlayName: string;
  entityName: string;
  attributes: string[];
  groups: Group[];
}

export function generateQueryParameters(
  cohort: Cohort
): tanagra.EntityDataset | null {
  const operands = cohort.groups
    .map((group) => generateFilter(group))
    .filter((filter) => filter) as Array<tanagra.Filter>;
  if (operands.length === 0) {
    return null;
  }

  return {
    entityVariable: "co",
    selectedAttributes: cohort.attributes,
    filter: {
      arrayFilter: {
        operands: operands,
        operator: tanagra.ArrayFilterOperator.And,
      },
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

function generateFilter(group: Group): tanagra.Filter | null {
  const operands = group.criteria
    .map((criteria) => getCriteriaPlugin(criteria).generateFilter())
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

// Having typed data here allows the registry to treat all data as unknown while
// plugins can use an actual type internally.
export interface CriteriaPlugin<DataType> {
  id: string;
  data: DataType;
  renderEdit: (cohort: Cohort, group: Group) => JSX.Element;
  renderDetails: () => JSX.Element;
  generateFilter: () => tanagra.Filter | null;
}

// registerCriteriaPlugin is a decorator that allows criteria to automatically
// register with the app simply by importing them.
export function registerCriteriaPlugin(
  type: string,
  title: string,
  defaultName: string
) {
  return <T extends CriteriaPluginConstructor>(constructor: T): void => {
    criteriaRegistry.set(type, {
      title,
      defaultName,
      constructor,
    });
  };
}

// In order to initialize the data for a newly created criteria, a plugin
// instance is created causing it to initialize its own data.
export function createCriteria(type: string): Criteria {
  const entry = getCriteriaEntry(type);
  const criteria = new entry.constructor(generateId(), null);
  return {
    id: criteria.id,
    type,
    name: entry.defaultName,
    count: 0,
    data: criteria.data,
  };
}

export function getCriteriaPlugin(criteria: Criteria): CriteriaPlugin<unknown> {
  return new (getCriteriaEntry(criteria.type).constructor)(
    criteria.id,
    criteria.data
  );
}

export function getCriteriaTitles(): { type: string; title: string }[] {
  return [...criteriaRegistry].map(([type, entry]) => ({
    type,
    title: entry.title,
  }));
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
  title: string;
  defaultName: string;
  constructor: CriteriaPluginConstructor;
};

const criteriaRegistry = new Map<string, RegistryEntry>();
