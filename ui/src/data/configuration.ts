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

export function findByID<T extends { id: string }>(id: string, list?: T[]): T {
  const item = list?.find((item) => item.id === id);
  if (!item) {
    throw new Error(`Unknown item "${id}" in ${JSON.stringify(list)}.`);
  }
  return item;
}
