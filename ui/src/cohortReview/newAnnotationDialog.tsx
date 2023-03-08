import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import { AnnotationType } from "data/source";
import { Select, TextField } from "mui-rff";
import { useState } from "react";
import { Form } from "react-final-form";

type Preset = {
  label: string;
  annotationType: AnnotationType;
  enumVals?: string[];
};

const presets: Preset[] = [
  {
    label: "Free text",
    annotationType: AnnotationType.String,
  },
  {
    label: "Review status",
    annotationType: AnnotationType.String,
    enumVals: ["Included", "Excluded", "Needs further review"],
  },
];

export type NewAnnotationDialogProps = {
  onCreate: (
    displayName: string,
    annotationType: AnnotationType,
    enumVals?: string[]
  ) => void;
};

// TODO(tjennison): Add validation.
export function useNewAnnotationDialog(
  props: NewAnnotationDialogProps
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
      aria-labelledby="new-annotation-dialog-title"
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle id="new-annotation-dialog-title">
        Create New Annotation
      </DialogTitle>
      <Form
        initialValues={{ preset: 0 }}
        onSubmit={({ displayName, preset }) => {
          setOpen(false);

          const p = presets[preset];
          props.onCreate(displayName, p.annotationType, p.enumVals);
        }}
        render={({ handleSubmit }) => (
          <form onSubmit={handleSubmit}>
            <DialogContent>
              <Select
                fullWidth
                required
                name="preset"
                label="Preset"
                data={presets.map((p, i) => ({ label: p.label, value: i }))}
              />
              <TextField
                autoFocus
                fullWidth
                required
                name="displayName"
                label="Display name"
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setOpen(false)}>Cancel</Button>
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
