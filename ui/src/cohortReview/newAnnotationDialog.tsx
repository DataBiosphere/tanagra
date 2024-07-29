import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import { AnnotationType } from "data/source";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { Select, TextField } from "mui-rff";
import { useState } from "react";
import { Form } from "react-final-form";

type Preset = {
  label: string;
  annotationType: AnnotationType;
  enumVals?: string[];
};

type FormData = {
  preset: number;
  displayName: string;
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

  const initialValues: FormData = {
    preset: 0,
    displayName: "",
  };

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
        initialValues={initialValues}
        validate={({ displayName }: FormData) => {
          if (!displayName) {
            return { displayName: "Display name may not be blank." };
          }
        }}
        onSubmit={({ displayName, preset }: FormData) => {
          setOpen(false);

          const p = presets[preset];
          props.onCreate(displayName, p.annotationType, p.enumVals);
        }}
        render={({ handleSubmit, invalid }) => (
          <form noValidate onSubmit={handleSubmit}>
            <DialogContent>
              <GridLayout rows>
                <GridBox sx={{ height: (theme) => theme.spacing(9) }}>
                  <Select
                    fullWidth
                    required
                    name="preset"
                    label="Preset"
                    data={presets.map((p, i) => ({ label: p.label, value: i }))}
                  />
                </GridBox>
                <GridBox sx={{ height: (theme) => theme.spacing(9) }}>
                  <TextField
                    autoFocus
                    fullWidth
                    required
                    name="displayName"
                    label="Display name"
                    inputProps={{
                      maxLength: 50,
                    }}
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
