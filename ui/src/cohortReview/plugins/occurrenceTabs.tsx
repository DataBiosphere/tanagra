import { useCohortReviewContext } from "cohortReview/cohortReviewContext";
import {
  CohortReviewPlugin,
  getCohortReviewPlugin,
  registerCohortReviewPlugin,
} from "cohortReview/pluginRegistry";
import { useReviewSearchState } from "cohortReview/reviewHooks";
import { GridBox } from "layout/gridBox";
import { useMemo } from "react";
import { CohortReviewPageConfig } from "underlaysSlice";
import { Tabs } from "components/tabs";

interface Config {
  entity: string;
  tabs: CohortReviewPageConfig[];
}

@registerCohortReviewPlugin("entityTabs")
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class _ implements CohortReviewPlugin {
  public entities: string[];
  private config: Config;

  constructor(
    public id: string,
    public title: string,
    config: CohortReviewPageConfig
  ) {
    this.config = config.plugin as Config;
    this.entities = this.config.tabs.reduce(
      (acc, t) => [...acc, getCohortReviewPlugin(t).entities[0]],
      [] as string[]
    );
  }

  render() {
    return <OccurrenceTabs config={this.config} />;
  }
}

function OccurrenceTabs({ config }: { config: Config }) {
  const context = useCohortReviewContext();
  if (!context) {
    return null;
  }

  const pagePlugins = useMemo(
    () => config.tabs.map((p) => getCohortReviewPlugin(p)),
    [config.tabs]
  );

  const [searchState, updateSearchState] = useReviewSearchState();

  const subTabPageId = searchState.subTabPageId ?? config.tabs[0].id;

  const changePage = (newValue: string) => {
    updateSearchState((state) => {
      state.subTabPageId = newValue;
    });
  };

  return (
    <GridBox
      sx={{
        width: "100%",
      }}
    >
      <Tabs
        configs={pagePlugins}
        currentTab={subTabPageId}
        setCurrentTab={changePage}
      />
    </GridBox>
  );
}
