import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import { TextField } from "mui-rff";
import { ReactNode, useState } from "react";
import { Form } from "react-final-form";

type TextInputDialogProps = {
  title: string;
  initialText?: string;
  textLabel: string;
  buttonLabel: string;
  onConfirm: (name: string) => void;
};

// Return a dialog and the callback function to show the dialog.
export function useTextInputDialog(
  props: TextInputDialogProps
): [ReactNode, () => void] {
  const [open, setOpen] = useState(false);
  const show = () => setOpen(true);

  return [
    // eslint-disable-next-line react/jsx-key
    <Dialog
      open={open}
      onClose={() => {
        setOpen(false);
      }}
      aria-labelledby="text-input-dialog-title"
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle id="text-input-dialog-title">{props.title}</DialogTitle>
      <Form
        onSubmit={({ text }) => {
          setOpen(false);
          props.onConfirm(text ?? "");
        }}
        initialValues={{
          text: props.initialText,
        }}
        render={({ handleSubmit }) => (
          <form onSubmit={handleSubmit}>
            <DialogContent>
              <TextField
                autoFocus
                fullWidth
                name="text"
                label={props.textLabel}
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" variant="contained">
                {props.buttonLabel}
              </Button>
            </DialogActions>
          </form>
        )}
      />
    </Dialog>,
    show,
  ];
}
