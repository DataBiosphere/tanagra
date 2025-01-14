import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import FormControl from "@mui/material/FormControl";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { uncontainedSelectSx } from "components/select";
import { Cohort } from "data/source";
import { useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode } from "react";
import { VizContainer } from "viz/vizContainer";
import { useState } from "react";
import Empty from "components/empty";
import { Underlay } from "underlaysSlice";

export type DemographicChartsProps = {
  cohort?: Cohort;
  extraControls?: ReactNode;
};

export function DemographicCharts(props: DemographicChartsProps) {
  const underlay = useUnderlay();

  const [selectedVisualizations, setSelectedVisualizations] = useState(
    getInitialVisualizations(underlay)
  );

  const onSelect = (event: SelectChangeEvent<string[]>) => {
    const {
      target: { value: sel },
    } = event;
    if (typeof sel === "string") {
      // This case is only for selects with text input.
      return;
    }

    setSelectedVisualizations(sel);
    storeSelectedVisualizations(underlay.name, sel);
  };

  return (
    <Paper
      sx={{
        p: 2,
        minHeight: "400px",
      }}
    >
      <GridLayout rows spacing={2}>
        <GridLayout cols fillCol={0} spacing={1} rowAlign="middle">
          <Typography variant="h6">Cohort visualizations</Typography>
          <FormControl>
            <Select
              fullWidth
              multiple
              displayEmpty
              value={selectedVisualizations}
              input={<OutlinedInput />}
              renderValue={(sel) => (
                <Typography variant="body2">{`${sel.length} visualizations selected`}</Typography>
              )}
              onChange={onSelect}
              sx={uncontainedSelectSx()}
            >
              {underlay.visualizations.map((viz) => {
                return (
                  <MenuItem key={viz.name} value={viz.name}>
                    {viz.title ?? "Unknown"}
                  </MenuItem>
                );
              })}
            </Select>
          </FormControl>
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
          {selectedVisualizations.length > 0 ? (
            underlay.visualizations.map((v) =>
              selectedVisualizations.find((sv) => sv === v.name) &&
              props.cohort ? (
                <VizContainer key={v.name} config={v} cohort={props.cohort} />
              ) : null
            )
          ) : (
            <Empty
              maxWidth="90%"
              minHeight="200px"
              title="No visualizations selected"
            />
          )}
        </GridBox>
      </GridLayout>
    </Paper>
  );
}

// TODO(tjennison): Store the selected visualizations in local storage per
// underlay for now.  Longer term, these should be stored on the backend but
// there a few options about whether they should be stored attached to the
// cohort, study, user, etc. as part of the visualiation changes.
function storageKey(underlay: string) {
  return `tanagra-selected-visualizations-${underlay}`;
}

function loadSelectedVisualizations(underlay: string): string[] | undefined {
  const stored = localStorage.getItem(storageKey(underlay));
  if (!stored) {
    return undefined;
  }
  return JSON.parse(stored);
}

function storeSelectedVisualizations(underlay: string, sel: string[]) {
  localStorage.setItem(storageKey(underlay), JSON.stringify(sel));
}

function getInitialVisualizations(underlay: Underlay) {
  const stored = loadSelectedVisualizations(underlay.name);
  if (!stored) {
    return (
      underlay.uiConfiguration.defaultVisualizations ??
      underlay.visualizations.map((viz) => viz.name)
    );
  }

  return stored.filter((v) =>
    underlay.visualizations.find((viz) => viz.name === v)
  );
}
