export enum SortDirection {
  Asc = "ASC",
  Desc = "DESC",
}

export const ROLLUP_COUNT_ATTRIBUTE = "t_rollup_count";
export const ITEM_COUNT_ATTRIBUTE = "t_item_count";
export const VALUE_SUFFIX = "_t_value";

export type SortOrder = {
  attribute: string;
  direction: SortDirection;
};

// Classifications allow occurrences to be filtered using an attribute that
// refers to another table. An example of this is how OMOP data stores the
// condition associated with a condition occurrence as a concept_id that
// references a concept table.
export type Classification = {
  id: string;
  attribute: string;

  entity: string;
  entityAttribute: string;
  hierarchy?: string;

  defaultSort: SortOrder;

  groupings?: Grouping[];
};

export type Grouping = {
  id: string;
  entity: string;
  defaultSort?: SortOrder;

  attributes?: string[];
};

export type Entity = {
  entity: string;
  key: string;

  classifications?: Classification[];
};

export type PrimaryEntity = Entity;

export type Occurrence = Entity & {
  id: string;
};

export type Configuration = {
  primaryEntity: PrimaryEntity;
  occurrences?: Occurrence[];
};

export function findByID<T extends { id: string }>(id: string, list?: T[]): T {
  const item = list?.find((item) => item.id === id);
  if (!item) {
    throw new Error(`Unknown item "${id}" in ${JSON.stringify(list)}.`);
  }
  return item;
}

export function findEntity(
  occurrenceID: string,
  config: Configuration
): Entity {
  if (occurrenceID) {
    return findByID(occurrenceID, config.occurrences);
  }
  return config.primaryEntity;
}
