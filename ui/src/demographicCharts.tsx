import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import Loading from "components/loading";
import { Cohort } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { useStudyId, useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode, useCallback } from "react";
import useSWRImmutable from "swr/immutable";
import { VizContainer } from "viz/vizContainer";
import { generateCohortFilter } from "./cohort";

export type DemographicChartsProps = {
  cohort: Cohort;
  separateCharts?: boolean;
  extraControls?: ReactNode;
};

export function DemographicCharts({
  cohort,
  extraControls,
}: DemographicChartsProps) {
  const underlay = useUnderlay();
  const underlaySource = useUnderlaySource();
  const studyId = useStudyId();
  const studySource = useStudySource();

  const fetchCount = useCallback(async () => {
    return (
      (process.env.REACT_APP_BACKEND_FILTERS
        ? await studySource.cohortCount(
            studyId,
            cohort.id,
            undefined,
            undefined,
            []
          )
        : await underlaySource.filterCount(
            generateCohortFilter(underlaySource, cohort),
            []
          ))?.[0]?.count ?? 0
    );
  }, [underlay, cohort]);

  const countState = useSWRImmutable(
    { component: "DemographicCharts", underlayName: underlay.name, cohort },
    fetchCount
  );

  return (
    <>
      <GridLayout rows spacing={3}>
        <GridLayout cols fillCol={2} rowAlign="bottom">
          <Typography variant="h6">Total count:&nbsp;</Typography>
          <Loading size="small" status={countState}>
            <Typography variant="h6">
              {countState.data?.toLocaleString()}
            </Typography>
          </Loading>
          <GridBox />
          {extraControls}
        </GridLayout>
        <Paper
          sx={{
            p: 2,
            minHeight: "400px",
          }}
        >
          {<VizContainer configs={underlay.visualizations} cohort={cohort} />}
        </Paper>
      </GridLayout>
    </>
  );
}
