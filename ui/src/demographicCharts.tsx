import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { generateCohortFilter } from "cohort";
import Loading from "components/loading";
import { Cohort } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { getEnvironment } from "environment";
import { useStudyId, useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode, useCallback } from "react";
import useSWRImmutable from "swr/immutable";
import { VizContainer } from "viz/vizContainer";

export type DemographicChartsProps = {
  cohort?: Cohort;
  extraControls?: ReactNode;
};

export function DemographicCharts(props: DemographicChartsProps) {
  const underlay = useUnderlay();
  const underlaySource = useUnderlaySource();
  const studyId = useStudyId();
  const studySource = useStudySource();

  const fetchCount = useCallback(async () => {
    if (!props.cohort) {
      return 0;
    }

    return (
      (getEnvironment().REACT_APP_BACKEND_FILTERS
        ? await studySource.cohortCount(
            studyId,
            props.cohort.id,
            undefined,
            undefined,
            []
          )
        : await underlaySource.filterCount(
            generateCohortFilter(underlaySource, props.cohort),
            []
          ))?.[0]?.count ?? 0
    );
  }, [underlay, props.cohort]);

  const countState = useSWRImmutable(
    {
      component: "DemographicCharts",
      underlayName: underlay.name,
      cohort: props.cohort,
    },
    fetchCount
  );

  return (
    <Paper
      sx={{
        p: 2,
        minHeight: "400px",
      }}
    >
      <GridLayout rows spacing={2}>
        <GridLayout cols fillCol={1} rowAlign="middle">
          <GridLayout rows>
            <Typography variant="h6">Cohort visualizations</Typography>
            <GridLayout cols fillCol={2} rowAlign="bottom">
              <Loading size="small" status={countState}>
                <Typography variant="body1">
                  {countState.data?.toLocaleString()} participants
                </Typography>
              </Loading>
            </GridLayout>
          </GridLayout>
          <GridBox />
          {props.extraControls}
        </GridLayout>
        <GridBox
          sx={{
            display: "grid",
            gridAutoRows: "380px",
            gridTemplateColumns: "repeat(auto-fill, minmax(400px, 1fr))",
            alignItems: "stretch",
            justifyItems: "stretch",
            gridGap: (theme) => theme.spacing(3),
          }}
        >
          {underlay.visualizations.map((v) =>
            props.cohort ? (
              <VizContainer key={v.name} config={v} cohort={props.cohort} />
            ) : null
          )}
        </GridBox>
      </GridLayout>
    </Paper>
  );
}
