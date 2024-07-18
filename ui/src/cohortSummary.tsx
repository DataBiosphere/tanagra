import Typography from "@mui/material/Typography";
import {
  getCriteriaPlugin,
  getCriteriaTitle,
  getCriteriaTitleFull,
} from "cohort";
import { Cohort, GroupSectionFilterKind } from "data/source";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";

export type CohortSummaryProps = {
  cohort: Cohort;
};

export function CohortSummary(props: CohortSummaryProps) {
  return (
    <GridLayout rows height="auto">
      {props.cohort.groupSections.map((s, si) => (
        <GridLayout key={s.id} rows height="auto">
          <Typography
            variant="body2em"
            component="p"
            sx={{ "::first-letter": { textTransform: "capitalize" } }}
          >
            {si != 0 ? "and " : ""}
            {s.filter.excluded ? "exclude " : "include "}participants with
            {s.filter.kind === GroupSectionFilterKind.Any ? " any " : " all "}
            of the following criteria:
          </Typography>
          {s.groups.map((g, gi) => (
            <GridBox key={g.id} sx={{ height: "auto" }}>
              <Typography variant="body2" component="span">
                {getCriteriaTitleFull(g.criteria[0])}
              </Typography>
              {g.criteria.length > 1 ? (
                <span>
                  <Typography variant="body2em" component="span">
                    {" where "}
                  </Typography>
                  <Typography variant="body2" component="span">
                    {g.criteria
                      .slice(1)
                      .map((c) =>
                        getCriteriaTitle(c, getCriteriaPlugin(c, g.entity))
                      )
                      .join(", ")}
                  </Typography>
                </span>
              ) : null}
              {gi != s.groups.length - 1 ? (
                <Typography variant="body2em" component="span">
                  {s.filter.kind === GroupSectionFilterKind.Any
                    ? " or "
                    : " and "}
                </Typography>
              ) : null}
            </GridBox>
          ))}
        </GridLayout>
      ))}
    </GridLayout>
  );
}
