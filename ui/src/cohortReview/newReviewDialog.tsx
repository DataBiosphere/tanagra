import { formatNumber, parseNumber } from "@brightspace-ui/intl/lib/number.js";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { TextField } from "mui-rff";
import { useState } from "react";
import { Form } from "react-final-form";

export type NewReviewDialogProps = {
  onCreate: (name: string, size: number) => void;
};

type FormData = {
  name: string;
  size: string;
};

const MAX = 10000;

export function useNewReviewDialog(
  props: NewReviewDialogProps
): [JSX.Element, () => void] {
  const [open, setOpen] = useState(false);
  const show = () => setOpen(true);

  return [
    // eslint-disable-next-line react/jsx-key
    <Dialog
      open={open}
      onClose={() => {
        setOpen(false);
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
          }

          if (!size) {
            ret.size = "Size may not be empty.";
          } else {
            const s = Math.floor(parseNumber(size));
            if (Number.isNaN(s)) {
              ret.size = "Size must be a number.";
            } else if (s <= 0) {
              ret.size = "Size must be greater than 0.";
            } else if (s > MAX) {
              ret.size = `Size may not be greater than ${formatNumber(MAX)}.`;
            }
          }

          if (Object.keys(ret).length > 0) {
            return ret;
          }
        }}
        onSubmit={({ name, size }: FormData) => {
          setOpen(false);
          props.onCreate(name, Math.floor(parseNumber(size)));
        }}
        render={({ handleSubmit, invalid }) => (
          <form noValidate onSubmit={handleSubmit}>
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
                    label={`Participant Count (max ${formatNumber(MAX)})`}
                    autoComplete="off"
                  />
                </GridBox>
              </GridLayout>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" variant="contained" disabled={invalid}>
                Create
              </Button>
            </DialogActions>
          </form>
        )}
      />
    </Dialog>,
    show,
  ];
}
