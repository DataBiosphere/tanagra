import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Typography from "@mui/material/Typography";
import { ReactNode, useState } from "react";

export type SimpleDialogConfig = {
  title: string;
  text: string;
  buttons: string[];
  primaryButtonColor?:
    | "inherit"
    | "primary"
    | "secondary"
    | "success"
    | "error"
    | "info"
    | "warning";
  onButton: (button: number) => void;
};

// Return a dialog and the callback function to show the dialog.
export function useSimpleDialog(): [
  ReactNode | null,
  (config: SimpleDialogConfig) => void
] {
  const [config, setConfig] = useState<SimpleDialogConfig | null>(null);
  const show = (config: SimpleDialogConfig) => setConfig(config);

  return [
    // eslint-disable-next-line react/jsx-key
    !!config ? (
      <Dialog
        open={!!config}
        onClose={() => {
          setConfig(null);
        }}
        aria-labelledby="simple-dialog-title"
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle id="simple-dialog-title">{config.title}</DialogTitle>
        <DialogContent>
          <Typography variant="body1">{config.text}</Typography>
        </DialogContent>
        <DialogActions>
          {config.buttons.map((b, i) => (
            <Button
              key={i}
              variant={
                i === config.buttons.length - 1 ? "contained" : undefined
              }
              color={
                i === config.buttons.length - 1
                  ? config.primaryButtonColor
                  : undefined
              }
              onClick={() => {
                setConfig(null);
                config.onButton(i);
              }}
            >
              {b}
            </Button>
          ))}
        </DialogActions>
      </Dialog>
    ) : null,
    show,
  ];
}
