import * as sortOrderProto from "proto/sort_order";

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

export function fromProtoSortOrder(
  sortOrder: sortOrderProto.SortOrder
): SortOrder {
  return {
    attribute: sortOrder.attribute,
    direction:
      sortOrder.direction ===
      sortOrderProto.SortOrder_Direction.SORT_ORDER_DIRECTION_DESCENDING
        ? SortDirection.Desc
        : SortDirection.Asc,
  };
}

export const DEFAULT_SORT_ORDER: sortOrderProto.SortOrder = {
  attribute: ROLLUP_COUNT_ATTRIBUTE,
  direction: sortOrderProto.SortOrder_Direction.SORT_ORDER_DIRECTION_DESCENDING,
};

export function findByID<T extends { id: string }>(id: string, list?: T[]): T {
  const item = list?.find((item) => item.id === id);
  if (!item) {
    throw new Error(`Unknown item "${id}" in ${JSON.stringify(list)}.`);
  }
  return item;
}
