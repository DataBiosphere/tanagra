import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { Cohort } from "data/source";
import { useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode } from "react";
import { VizContainer } from "viz/vizContainer";

export type DemographicChartsProps = {
  cohort?: Cohort;
  extraControls?: ReactNode;
};

export function DemographicCharts(props: DemographicChartsProps) {
  const underlay = useUnderlay();

  return (
    <Paper
      sx={{
        p: 2,
        minHeight: "400px",
      }}
    >
      <GridLayout rows spacing={2}>
        <GridLayout cols fillCol={1} rowAlign="middle">
          <Typography variant="h6">Cohort visualizations</Typography>
          <GridBox />
          {props.extraControls}
        </GridLayout>
        <GridBox
          sx={{
            display: "grid",
            gridAutoRows: "380px",
            gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))",
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
