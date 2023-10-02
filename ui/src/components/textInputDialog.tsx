import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { TextField } from "mui-rff";
import { ReactNode, useState } from "react";
import { Form } from "react-final-form";

type FormData = {
  text: string;
};

type TextInputDialogConfig = {
  title: string;
  initialText?: string;
  textLabel: string;
  buttonLabel: string;
  onConfirm: (name: string) => void;
};

type TextInputDialogConfigInternal = TextInputDialogConfig & {
  initialValues: FormData;
};

// Return a dialog and the callback function to show the dialog.
export function useTextInputDialog(): [
  ReactNode,
  (config: TextInputDialogConfig) => void
] {
  const [config, setConfig] = useState<TextInputDialogConfigInternal | null>(
    null
  );
  const show = (config: TextInputDialogConfig) => {
    setConfig({
      initialValues: {
        text: config.initialText ?? "",
      },
      ...config,
    });
  };

  return [
    // eslint-disable-next-line react/jsx-key
    !!config ? (
      <Dialog
        open={!!config}
        onClose={() => {
          setConfig(null);
        }}
        aria-labelledby="text-input-dialog-title"
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle id="text-input-dialog-title">{config.title}</DialogTitle>
        <Form
          onSubmit={({ text }: FormData) => {
            setConfig(null);
            config.onConfirm(text ?? "");
          }}
          initialValues={config.initialValues}
          validate={({ text }: FormData) => {
            if (!text) {
              return { text: `${config.textLabel} may not be blank.` };
            }
          }}
          render={({ handleSubmit, invalid }) => (
            <form noValidate onSubmit={handleSubmit}>
              <DialogContent>
                <GridLayout rows>
                  <GridBox sx={{ height: (theme) => theme.spacing(9) }}>
                    <TextField
                      autoFocus
                      fullWidth
                      name="text"
                      label={config.textLabel}
                    />
                  </GridBox>
                </GridLayout>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setConfig(null)}>Cancel</Button>
                <Button type="submit" variant="contained" disabled={invalid}>
                  {config.buttonLabel}
                </Button>
              </DialogActions>
            </form>
          )}
        />
      </Dialog>
    ) : null,
    show,
  ];
}
