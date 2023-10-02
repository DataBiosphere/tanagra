import { useCohortReviewContext } from "cohortReview/cohortReviewContext";
import {
  CohortReviewPlugin,
  registerCohortReviewPlugin,
} from "cohortReview/pluginRegistry";
import {
  TreeGrid,
  TreeGridColumn,
  TreeGridData,
  TreeGridSortDirection,
  TreeGridSortOrder,
} from "components/treegrid";
import {
  compareDataValues,
  DataKey,
  DataValue,
  stringifyDataValue,
} from "data/types";
import { produce } from "immer";
import { GridBox } from "layout/gridBox";
import { useMemo } from "react";
import { CohortReviewPageConfig } from "underlaysSlice";

interface Config {
  occurrence: string;
  columns: TreeGridColumn[];
}

@registerCohortReviewPlugin("occurrenceTable")
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CohortReviewPlugin {
  public occurrences: string[];
  private config: Config;

  constructor(
    public id: string,
    public title: string,
    config: CohortReviewPageConfig
  ) {
    this.config = config.plugin as Config;
    this.occurrences = [this.config.occurrence];
  }

  render() {
    return <OccurrenceTable id={this.id} config={this.config} />;
  }
}

type SearchState = {
  sortOrders?: TreeGridSortOrder[];
  columnFilters?: { [key: string]: string };
};

function OccurrenceTable({ id, config }: { id: string; config: Config }) {
  const context = useCohortReviewContext();
  if (!context) {
    return null;
  }

  const searchState = context.searchState<SearchState>(id);

  const data = useMemo(() => {
    const children: DataKey[] = [];
    const data: TreeGridData = {
      root: { data: {}, children },
    };

    context.occurrences[config.occurrence].forEach((o) => {
      data[o.key] = { data: o };
      children.push(o.key);
    });

    return data;
  }, [context]);

  const filterRegExps = useMemo(() => {
    const regexps: { [key: string]: RegExp } = {};
    for (const key in searchState.columnFilters) {
      try {
        regexps[key] = new RegExp(searchState.columnFilters[key], "i");
      } catch (e) {
        // TODO(tjennison): Show error messages for invalid regexps.
      }
    }
    return regexps;
  }, [searchState.columnFilters]);

  const sortedData = useMemo(() => {
    return produce(data, (data) => {
      if (!data.root?.children) {
        return;
      }

      data.root.children = data.root.children.filter((child) =>
        Object.entries(filterRegExps ?? {}).reduce(
          (cur: boolean, [col, re]) =>
            cur &&
            re.test(stringifyDataValue(data[child].data[col] as DataValue)),
          true
        )
      );
      data.root.children.sort((a, b) => {
        for (const o of searchState.sortOrders ?? []) {
          const valA = data[a].data[o.column] as DataValue | undefined;
          const valB = data[b].data[o.column] as DataValue | undefined;
          const c = compareDataValues(valA, valB);
          if (c !== 0) {
            return o.direction === TreeGridSortDirection.Asc ? c : -c;
          }
        }

        return 0;
      });
    });
  }, [data, searchState]);

  return (
    <GridBox
      sx={{
        width: "100%",
      }}
    >
      <TreeGrid
        columns={config.columns}
        data={sortedData}
        sortOrders={searchState.sortOrders}
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
    </GridBox>
  );
}
