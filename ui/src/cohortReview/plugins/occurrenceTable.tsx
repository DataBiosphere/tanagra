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
import { compareDataValues, DataKey, DataValue } from "data/types";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import { useMemo, useState } from "react";
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
    return <OccurrenceTable config={this.config} />;
  }
}

function OccurrenceTable({ config }: { config: Config }) {
  const [sortOrders, setSortOrders] = useState<TreeGridSortOrder[]>([]);

  const context = useCohortReviewContext();
  if (!context) {
    return null;
  }

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

  const sortedData = useMemo(() => {
    return produce(data, (data) => {
      data.root?.children?.sort((a, b) => {
        for (const o of sortOrders) {
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
  }, [data, sortOrders]);

  return (
    <GridBox
      sx={{
        width: "100%",
      }}
    >
      <TreeGrid
        columns={config.columns}
        data={sortedData}
        onSort={setSortOrders}
      />
    </GridBox>
  );
}
