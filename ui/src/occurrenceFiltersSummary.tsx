import ViewColumnIcon from "@mui/icons-material/ViewColumn";
import Typography from "@mui/material/Typography";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { OccurrenceFilters } from "./cohort";

export type OccurrenceFiltersSummaryProps = {
  filters: OccurrenceFilters;
};

export function OccurrenceFiltersSummary(props: OccurrenceFiltersSummaryProps) {
  return (
    <GridLayout rows height="auto">
      <GridBox>
        <Typography
          variant="body2em"
          component="span"
          sx={{ textTransform: "capitalize" }}
        >
          {props.filters.name}
        </Typography>
        <Typography variant="body2" component="span">
          {" table contains "}
        </Typography>
        <Typography variant="body2em" component="span">
          {[...props.filters.sourceCriteria].sort().join(", ")}
        </Typography>
      </GridBox>
      <GridLayout cols rowAlign="middle">
        <ViewColumnIcon sx={{ display: "block" }} />
        <Typography variant="body2" component="span">
          Columns include
        </Typography>
      </GridLayout>
      <GridBox>
        <Typography variant="body2" component="span">
          {[...props.filters.attributes].sort().join("; ")}
        </Typography>
      </GridBox>
    </GridLayout>
  );
}
