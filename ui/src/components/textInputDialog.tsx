import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import TextField from "@mui/material/TextField";
import { ChangeEvent, ReactNode, useState } from "react";

type textInputDialogProps = {
  title: string;
  titleId: string;
  textLabel: string;
  buttonHint: string;
  callback: (name: string) => void;
};

export function useTextInputDialog(
  props: textInputDialogProps
): [ReactNode, () => void] {
  const [open, setOpen] = useState(false);
  const show = () => {
    setOpen(true);
  };

  const [name, setName] = useState(props.title);
  const onNameChange = (event: ChangeEvent<HTMLInputElement>) => {
    setName(event.target.value);
  };

  const onConfirm = () => {
    setOpen(false);
    props.callback(name);
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
          id="name"
          label={props.textLabel}
          fullWidth
          variant="standard"
          value={name}
          onChange={onNameChange}
        />
      </DialogContent>
      <DialogActions>
        <Button
          variant="contained"
          disabled={name.length === 0}
          onClick={onConfirm}
        >
          {props.buttonHint}
        </Button>
      </DialogActions>
    </Dialog>,
    show,
  ];
}
