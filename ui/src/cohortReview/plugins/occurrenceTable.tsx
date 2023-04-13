import { useCohortReviewContext } from "cohortReview/cohortReviewContext";
import {
  CohortReviewPlugin,
  registerCohortReviewPlugin,
} from "cohortReview/pluginRegistry";
import { TreeGrid, TreeGridColumn, TreeGridData } from "components/treegrid";
import { DataKey } from "data/types";
import { GridBox } from "layout/gridBox";
import React, { useMemo } from "react";
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

  return (
    <GridBox
      sx={{
        width: "100%",
        p: 1,
      }}
    >
      <TreeGrid columns={config.columns} data={data} />
    </GridBox>
  );
}
