import { formatNumber, parseNumber } from "@brightspace-ui/intl/lib/number.js";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Loading from "components/loading";
import { Cohort, MAX_DISPLAY_NAME_LENGTH } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useStudyId } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { TextField } from "mui-rff";
import { Form } from "react-final-form";
import useSWRImmutable from "swr/immutable";
import { isValid } from "util/valid";

export type UseNewReviewDialogProps = {
  cohort: Cohort;
  onCreate: (name: string, size: number) => void;
};

type FormData = {
  name: string;
  size: string;
};

// BigQuery has a parameter limit of 10_000. Cohort review creations stores
// review primaryEntityIds in table primary_entity_instance, and uses the Ids
// from this table as params in a select against ENT_primary_entity.
// `@currentTimestamp` is already a param in the select. Hence, limit the max
// review size to 9_999 (10_000 - 1 for @currentTimestamp). Same value in
// service/src/main/java/bio/terra/tanagra/service/artifact/ReviewService.java:MAX_REVIEW_SIZE
const MAX_REVIEW_SIZE = 9_999;

export type NewReviewDialogProps = {
  open: boolean;
  setOpen: (open: boolean) => void;
} & UseNewReviewDialogProps;

export function NewReviewDialog(props: NewReviewDialogProps) {
  const studyId = useStudyId();
  const studySource = useStudySource();

  const countState = useSWRImmutable(
    {
      type: "count",
      cohort: props.cohort,
    },
    async () =>
      (await studySource.cohortCount(studyId, props.cohort.id))?.[0]?.count ?? 0
  );

  const cohortCount = countState.data;
  const max = isValid(cohortCount)
    ? Math.min(MAX_REVIEW_SIZE, cohortCount)
    : MAX_REVIEW_SIZE;

  return (
    <Dialog
      open={props.open}
      onClose={() => {
        props.setOpen(false);
      }}
      aria-labelledby="new-review-dialog-title"
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle id="new-review-dialog-title">
        Create New Cohort Review
      </DialogTitle>
      <Form
        validate={({ name, size }: FormData) => {
          const ret: Record<string, string> = {};
          if (!name) {
            ret.name = "Name may not be empty.";
          } else if (name.trim().length > MAX_DISPLAY_NAME_LENGTH) {
            ret.name = `Name may not exceed ${MAX_DISPLAY_NAME_LENGTH} characters`;
          }

          if (!size) {
            ret.size = "Size may not be empty.";
          } else {
            const s = Math.floor(parseNumber(size));
            if (Number.isNaN(s)) {
              ret.size = "Size must be a number.";
            } else if (s <= 0) {
              ret.size = "Size must be greater than 0.";
            } else if (s > max) {
              ret.size = `Size may not be greater than ${formatNumber(max)}.`;
            }
          }

          if (Object.keys(ret).length > 0) {
            return ret;
          }
        }}
        onSubmit={({ name, size }: FormData) => {
          props.setOpen(false);
          props.onCreate(name, Math.floor(parseNumber(size)));
        }}
        render={({ handleSubmit, invalid }) => (
          <form noValidate onSubmit={handleSubmit}>
            <Loading status={countState}>
              <DialogContent>
                <GridLayout rows>
                  <GridBox sx={{ height: (theme) => theme.spacing(9) }}>
                    <TextField
                      autoFocus
                      fullWidth
                      name="name"
                      label="Cohort Review Name"
                      autoComplete="off"
                    />
                  </GridBox>
                  <GridBox sx={{ height: (theme) => theme.spacing(9) }}>
                    <TextField
                      fullWidth
                      name="size"
                      label={`Participant Count (max ${maxDisplay(
                        max,
                        cohortCount
                      )})`}
                      autoComplete="off"
                    />
                  </GridBox>
                </GridLayout>
              </DialogContent>
            </Loading>
            <DialogActions>
              <Button onClick={() => props.setOpen(false)}>Cancel</Button>
              <Button type="submit" variant="contained" disabled={invalid}>
                Create
              </Button>
            </DialogActions>
          </form>
        )}
      />
    </Dialog>
  );
}

function maxDisplay(max: number, cohortCount?: number) {
  if (!cohortCount) {
    return formatNumber(max);
  } else if (cohortCount > MAX_REVIEW_SIZE) {
    return `${formatNumber(MAX_REVIEW_SIZE)} of ${formatNumber(cohortCount)}`;
  }
  return max;
}
