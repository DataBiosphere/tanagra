import { useCohortReviewContext } from "cohortReview/cohortReviewContext";
import {
  CohortReviewPlugin,
  getCohortReviewPlugin,
  registerCohortReviewPlugin,
} from "cohortReview/pluginRegistry";
import { useReviewSearchState } from "cohortReview/reviewHooks";
import { GridBox } from "layout/gridBox";
import { useEffect, useMemo } from "react";
import { CohortReviewPageConfig } from "underlaysSlice";
import { Tabs } from "components/tabs";

interface Config {
  entity: string;
  tabs: CohortReviewPageConfig[];
}

@registerCohortReviewPlugin("entityTabs")
class _ implements CohortReviewPlugin {
  public entities: string[];
  private config: Config;

  constructor(
    public id: string,
    public title: string,
    config: CohortReviewPageConfig
  ) {
    this.config = config.plugin as Config;
    this.entities = this.config.tabs.map(
      (t) => getCohortReviewPlugin(t).entities[0]
    );
  }

  render() {
    return <OccurrenceTabs config={this.config} />;
  }
}

export function OccurrenceTabs({ config }: { config: Config }) {
  const context = useCohortReviewContext();

  const pagePlugins = useMemo(
    () => config.tabs.map((p) => getCohortReviewPlugin(p)),
    [config.tabs]
  );

  const [searchState, updateSearchState] = useReviewSearchState();

  useEffect(() => {
    if (!searchState.subTabPageId) {
      updateSearchState((state) => {
        state.subTabPageId = config.tabs[0].id;
      });
    }
  }, []);

  if (!context) {
    return null;
  }

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
        currentTab={searchState.subTabPageId}
        setCurrentTab={changePage}
      />
    </GridBox>
  );
}
