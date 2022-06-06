import { generate } from "randomstring";
import * as tanagra from "tanagra-api";
import { CriteriaConfig, Underlay } from "./underlaysSlice";

export function generateId(): string {
  return generate(8);
}

export function generateQueryFilter(
  cohort: tanagra.Cohort,
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

function generateFilter(
  group: tanagra.Group,
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

  const filter = {
    arrayFilter: {
      operands: operands,
      operator: tanagra.ArrayFilterOperator.Or,
    },
  };

  return group.kind === tanagra.GroupKindEnum.Included
    ? filter
    : {
        unaryFilter: {
          operand: filter,
          operator: tanagra.UnaryFilterOperator.Not,
        },
      };
}

// Having typed data here allows the registry to treat all data generically
// while plugins can use an actual type internally.
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
  initializeData: (underlay: Underlay, config: CriteriaConfig) => object
) {
  return <T extends CriteriaPluginConstructor>(constructor: T): void => {
    criteriaRegistry.set(type, {
      initializeData,
      constructor,
    });
  };
}

export function createCriteria(
  underlay: Underlay,
  config: CriteriaConfig
): tanagra.Criteria {
  const entry = getCriteriaEntry(config.type);
  return {
    id: generateId(),
    type: config.type,
    name: config.defaultName,
    data: entry.initializeData(underlay, config),
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
  initializeData: (underlay: Underlay, config: CriteriaConfig) => object;
  constructor: CriteriaPluginConstructor;
};

const criteriaRegistry = new Map<string, RegistryEntry>();
