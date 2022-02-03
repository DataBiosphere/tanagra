import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import FormControl from "@mui/material/FormControl";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { insertCohort } from "cohortsSlice";
import { useAppDispatch, useAppSelector } from "hooks";
import { ChangeEvent, ReactNode, useCallback, useState } from "react";
import { Link as RouterLink } from "react-router-dom";
import ActionBar from "./actionBar";
import { useSqlDialog } from "./sqlDialog";

type DatasetProps = {
  underlayNames: string[];
  entityName: string;
};

export function Datasets(props: DatasetProps) {
  const dispatch = useAppDispatch();
  const cohorts = useAppSelector((state) => state.cohorts);

  const [selected, setSelected] = useState<string[]>([]);
  const [sqlDialog, showSqlDialog] = useSqlDialog({
    cohort: cohorts.find((c) => c.id === selected[0]),
  });

  const [dialog, show] = useNewCohortDialog({
    underlayNames: props.underlayNames,
    callback: (name: string, underlayName: string) => {
      dispatch(
        insertCohort(
          name,
          underlayName,
          props.entityName,
          // TODO(tjennison): Populate from an actual source.
          [
            "person_id",
            "condition_occurrence_id",
            "condition_concept_id",
            "condition_name",
          ]
        )
      );
    },
  });

  const onCheck = (id: string) => {
    const newSelected = selected.slice();
    const index = selected.indexOf(id);
    if (index > -1) {
      newSelected.splice(index, 1);
    } else {
      newSelected.push(id);
    }
    setSelected(newSelected);
  };

  return (
    <>
      <ActionBar title="Datasets" />
      <Grid container columns={2} className="datasets">
        <Grid item xs={1}>
          <Typography variant="h4">Cohorts</Typography>
          <Paper>
            {cohorts.map((cohort) => (
              <Box
                key={cohort.id}
                sx={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "baseline",
                }}
              >
                <Checkbox
                  checked={selected.includes(cohort.id)}
                  onChange={() => {
                    onCheck(cohort.id);
                  }}
                />
                <Typography variant="h5">{cohort.name}&nbsp;</Typography>
                <Typography variant="body2" sx={{ flexGrow: 1 }}>
                  ({cohort.underlayName})
                </Typography>
                <IconButton
                  color="inherit"
                  component={RouterLink}
                  to={`/cohort/${cohort.id}`}
                >
                  <EditIcon />
                </IconButton>
              </Box>
            ))}
          </Paper>
          <Button variant="contained" onClick={show}>
            New Cohort
          </Button>
          {dialog}
        </Grid>
        <Grid item xs={1}>
          <Typography variant="h4">Concept Sets</Typography>
          <Paper>
            <Box
              sx={{
                display: "flex",
                flexDirection: "row",
                alignItems: "baseline",
              }}
            >
              <Checkbox disabled checked />
              <Typography variant="h5">All Condition Occurrences</Typography>
            </Box>
          </Paper>
          <Button
            variant="contained"
            disabled={selected.length != 1}
            onClick={showSqlDialog}
          >
            Generate Query
          </Button>
          {sqlDialog}
        </Grid>
      </Grid>
    </>
  );
}

type NewCohortDialogProps = {
  underlayNames: string[];
  callback: (name: string, underlayName: string) => void;
};

function useNewCohortDialog(
  props: NewCohortDialogProps
): [ReactNode, () => void] {
  const [open, setOpen] = useState(false);
  const show = useCallback(() => {
    setOpen(true);
  }, []);

  const [name, setName] = useState("New Cohort");
  const onNameChange = (event: ChangeEvent<HTMLInputElement>) => {
    setName(event.target.value);
  };

  const [underlayName, setUnderlayName] = useState(props.underlayNames[0]);

  const onCreate = () => {
    setOpen(false);
    props.callback(name, underlayName);
  };

  const onUnderlayChange = (event: SelectChangeEvent<typeof underlayName>) => {
    setUnderlayName(event.target.value || "");
  };

  return [
    // eslint-disable-next-line react/jsx-key
    <Dialog
      open={open}
      onClose={() => {
        setOpen(false);
      }}
      aria-labelledby="new-cohort-dialog-title"
      maxWidth="sm"
      fullWidth
      className="new-cohort-dialog"
    >
      <DialogTitle id="new-cohort-dialog-title">New Cohort</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          margin="dense"
          id="name"
          label="Cohort Name"
          fullWidth
          variant="standard"
          value={name}
          onChange={onNameChange}
        />
        <FormControl>
          <InputLabel id="dataset-select-label">Dataset</InputLabel>
          <Select
            labelId="dataset-select-label"
            label="Dataset"
            value={underlayName}
            onChange={onUnderlayChange}
          >
            {props.underlayNames.map((underlayName) => (
              <MenuItem key={underlayName} value={underlayName}>
                {underlayName}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </DialogContent>
      <DialogActions>
        <Button
          variant="contained"
          disabled={name.length === 0}
          onClick={onCreate}
        >
          Create
        </Button>
      </DialogActions>
    </Dialog>,
    show,
  ];
}
