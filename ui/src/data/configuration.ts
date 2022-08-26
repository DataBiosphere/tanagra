import * as tanagra from "tanagra-api";

export type DataKey = string | number;
export type DataValue = string | number | boolean;

export type DataEntry = {
  key: DataKey;
  [x: string]: DataValue;
};

export enum SortDirection {
  Asc = "ASC",
  Desc = "DESC",
}

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
  hierarchical?: boolean;

  defaultSort?: SortOrder;

  groupings?: Grouping[];

  // TODO(tjennison): This isn't ideal. It would be better if the underlay
  // supported multiple hierarchies directly but I don't see an alternative to
  // this for now other than hardcoding the source/standard logic which seems
  // worse.
  filter?: tanagra.Filter;
};

export type Grouping = {
  id: string;
  entity: string;
  defaultSort?: SortOrder;
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
