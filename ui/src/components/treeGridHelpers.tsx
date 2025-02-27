import { useMemo } from "react";
import {
  TreeGridRowData,
  TreeGridData,
  TreeGridItem,
  TreeGridId,
  TreeGridColumn,
} from "./treeGrid";
import * as columnProto from "proto/column";

export function useArrayAsTreeGridData<
  T extends TreeGridRowData,
  K extends keyof T,
>(array: T[], key: K): TreeGridData<TreeGridItem<T>> {
  return useMemo(() => {
    const children: TreeGridId[] = [];
    const data = new Map<TreeGridId, TreeGridItem<T>>([
      // Force empty data here since it's never accessed but making it optional
      // is a pain for "data" access everywhere.
      ["root", { data: {} as T, children }],
    ]);

    array?.forEach((a) => {
      const k = a[key] as TreeGridId;
      data.set(k, { data: a });
      children.push(k);
    });

    return data;
  }, [array, key]);
}
export function fromProtoColumns(
  columns: columnProto.Column[]
): TreeGridColumn[] {
  return columns.map((c) => ({
    key: c.key,
    width: c.widthString ?? c.widthDouble ?? 100,
    title: c.title,
    sortable: c.sortable,
    filterable: c.filterable,
  }));
}
export enum TreeGridSortDirection {
  Asc = "ASC",
  Desc = "DESC",
}
