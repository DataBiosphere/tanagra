import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import { TextField } from "mui-rff";
import { useState } from "react";
import { Form } from "react-final-form";

export type NewReviewDialogProps = {
  onCreate: (name: string, size: number) => void;
};

// TODO(tjennison): Add validation.
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
        onSubmit={({ name, size }) => {
          setOpen(false);
          props.onCreate(name, size);
        }}
        render={({ handleSubmit }) => (
          <form onSubmit={handleSubmit}>
            <DialogContent>
              <TextField
                autoFocus
                fullWidth
                name="name"
                label="Cohort Review Name"
                autoComplete="off"
              />
              <TextField
                fullWidth
                name="size"
                label="Participant Count (max 10,000)"
                autoComplete="off"
              />
            </DialogContent>
            <DialogActions>
              <Button type="submit" variant="contained">
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
