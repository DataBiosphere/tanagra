import { useCohortReviewContext } from "cohortReview/cohortReviewContext";
import {
  CohortReviewPlugin,
  registerCohortReviewPlugin,
} from "cohortReview/pluginRegistry";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridSortOrder,
} from "components/treeGrid";
import { TreeGridSortDirection } from "components/treeGridHelpers";
import {
  compareDataValues,
  DataKey,
  DataValue,
  stringifyDataValue,
} from "data/types";
import { produce } from "immer";
import { GridBox } from "layout/gridBox";
import { useMemo, useState } from "react";
import { CohortReviewPageConfig } from "underlaysSlice";
import { safeRegExp } from "util/safeRegExp";
import { TablePagination } from "@mui/material";

interface Config {
  entity: string;
  columns: TreeGridColumn[];
}

@registerCohortReviewPlugin("entityTable")
class _ implements CohortReviewPlugin {
  public entities: string[];
  private config: Config;

  constructor(
    public id: string,
    public title: string,
    config: CohortReviewPageConfig
  ) {
    this.config = config.plugin as Config;
    this.entities = [this.config.entity];
  }

  render() {
    return <OccurrenceTable id={this.id} config={this.config} />;
  }
}

type SearchState = {
  sortOrders?: TreeGridSortOrder[];
  columnFilters?: { [key: string]: string };
};

export function OccurrenceTable({
  id,
  config,
}: {
  id: string;
  config: Config;
}) {
  const context = useCohortReviewContext();
  const searchState = context?.searchState<SearchState>(id);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [rowsPerPage, setRowsPerPage] = useState<number>(25);

  const data = useMemo(() => {
    const children: DataKey[] = [];
    const rows = new Map();

    context.rows[config.entity]?.forEach((o) => {
      rows.set(o.key, { data: o });
      children.push(o.key);
    });

    return {
      rows,
      children,
    };
  }, [context, config]);

  const filterRegExps = useMemo(() => {
    const regexps: { [key: string]: RegExp } = {};
    for (const key in searchState?.columnFilters) {
      regexps[key] = safeRegExp(searchState?.columnFilters[key])[0];
    }
    return regexps;
  }, [searchState.columnFilters]);

  const sortedData = useMemo(() => {
    return produce(data, (data) => {
      data.children = (data.children ?? []).filter((child) =>
        Object.entries(filterRegExps ?? {}).reduce(
          (cur: boolean, [col, re]) =>
            cur &&
            re.test(
              stringifyDataValue(data.rows.get(child)?.data?.[col] as DataValue)
            ),
          true
        )
      );
      data.children.sort((a, b) => {
        for (const o of searchState.sortOrders ?? []) {
          const valA = data.rows.get(a)?.data?.[o.column] as
            | DataValue
            | undefined;
          const valB = data.rows.get(b)?.data?.[o.column] as
            | DataValue
            | undefined;
          const c = compareDataValues(valA, valB);
          if (c !== 0) {
            return o.direction === TreeGridSortDirection.Asc ? c : -c;
          }
        }

        return 0;
      });
      data.children = data.children.slice(
        currentPage * rowsPerPage,
        (currentPage + 1) * rowsPerPage
      );
    });
  }, [data, searchState, filterRegExps, currentPage, rowsPerPage]);

  if (!context) {
    return null;
  }

  return (
    <GridBox
      sx={{
        width: "100%",
      }}
    >
      <TreeGrid
        columns={config.columns}
        data={sortedData}
        sortOrders={searchState.sortOrders ?? []}
        onSort={(sortOrders) => {
          context.updateSearchState(id, (state: SearchState) => {
            state.sortOrders = sortOrders;
          });
        }}
        filters={searchState.columnFilters}
        onFilter={(filters) => {
          context.updateSearchState(id, (state: SearchState) => {
            state.columnFilters = filters;
          });
        }}
      />
      <TablePagination
        component="div"
        count={data.rows.size}
        page={currentPage}
        rowsPerPage={rowsPerPage}
        onPageChange={(e, newPage) => setCurrentPage(newPage)}
        onRowsPerPageChange={(e) => {
          setRowsPerPage(parseInt(e.target.value, 10));
          setCurrentPage(0);
        }}
      />
    </GridBox>
  );
}
