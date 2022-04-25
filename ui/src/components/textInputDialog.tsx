import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import TextField from "@mui/material/TextField";
import { ChangeEvent, ReactNode, useState } from "react";

type TextInputDialogProps = {
  title: string;
  text?: string;
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

  const [text, setText] = useState(
    typeof props.text !== "undefined" ? props.text : props.title
  );
  const onTextChange = (event: ChangeEvent<HTMLInputElement>) => {
    setText(event.target.value);
  };

  const onConfirm = () => {
    setOpen(false);
    props.onConfirm(text);
  };

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
      className="text-input-dialog-name"
    >
      <DialogTitle id="text-input-dialog-title">{props.title}</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          margin="dense"
          id="text"
          label={props.textLabel}
          fullWidth
          variant="standard"
          value={text}
          onChange={onTextChange}
        />
      </DialogContent>
      <DialogActions>
        <Button
          variant="contained"
          disabled={text.length === 0}
          onClick={onConfirm}
        >
          {props.buttonLabel}
        </Button>
      </DialogActions>
    </Dialog>,
    show,
  ];
}
