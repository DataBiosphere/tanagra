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
    const rows = new Map<TreeGridId, TreeGridItem<T>>();

    array?.forEach((a) => {
      const k = a[key] as TreeGridId;
      rows.set(k, { data: a });
      children.push(k);
    });

    return {
      rows,
      children,
    };
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
