import Backdrop from "@mui/material/Backdrop";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Grid from "@mui/material/Grid";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useAdminSource } from "sd-admin/source";
import { CohortV2 } from "tanagra-api";

const columns = (
  filterFn: (name: string, value: string) => void
): GridColDef[] => [
  {
    field: "displayName",
    headerName: "Set Name",
    sortable: true,
    disableColumnMenu: true,
    flex: 1,
    renderHeader: () => (
      <div style={{ lineHeight: "1.5rem" }}>
        <div>Set Name</div>
        <div>
          <TextField
            sx={{
              "& .MuiInputBase-root": {
                margin: 0,
              },
              "& .MuiInputLabel-root": {
                top: "-16px",
              },
            }}
            name="displayName"
            onChange={({ target: { name, value } }) => filterFn(name, value)}
            label="Filter"
            variant="standard"
            size="small"
            margin="none"
          />
        </div>
      </div>
    ),
  },
  {
    field: "lastModified",
    headerName: "Last Modified",
    sortable: true,
    disableColumnMenu: true,
    width: 120,
    renderHeader: () => (
      <div style={{ lineHeight: "1.5rem" }}>
        <div>Last Modified</div>
        <div>
          <TextField
            sx={{
              "& .MuiInputBase-root": {
                margin: 0,
              },
              "& .MuiInputLabel-root": {
                top: "-16px",
              },
            }}
            name="lastModified"
            onChange={({ target: { name, value } }) => filterFn(name, value)}
            label="Filter"
            variant="standard"
            size="small"
            margin="none"
          />
        </div>
      </div>
    ),
  },
  {
    field: "studyName",
    headerName: "Study Name",
    sortable: true,
    disableColumnMenu: true,
    width: 120,
    renderHeader: () => (
      <div style={{ lineHeight: "1.5rem" }}>
        <div>Study Name</div>
        <div>
          <TextField
            sx={{
              "& .MuiInputBase-root": {
                margin: 0,
              },
              "& .MuiInputLabel-root": {
                top: "-16px",
              },
            }}
            name="studyName"
            onChange={({ target: { name, value } }) => filterFn(name, value)}
            label="Filter"
            variant="standard"
            size="small"
            margin="none"
          />
        </div>
      </div>
    ),
  },
];
const mapSetRows = ({
  id,
  displayName,
  description,
  underlayName,
  lastModified,
}: CohortV2) => ({
  id,
  displayName,
  description,
  underlayName,
  lastModified,
});
// Style override to slightly darken the text of disabled form fields
const DISABLED_SX = [
  {
    ".Mui-disabled": {
      color: "rgba(0, 0, 0, 0.6)",
      "-webkit-text-fill-color": "rgba(0, 0, 0, 0.6)",
    },
  },
];
const ROWS_PER_PAGE = 25;

const emptySet: CohortV2 = {
  id: "",
  displayName: "",
  description: "",
  underlayName: "",
  criteriaGroups: [],
  lastModified: new Date(),
};

const initialFormState = {
  displayName: {
    touched: false,
    value: "",
  },
  description: {
    touched: false,
    value: "",
  },
  underlayName: {
    touched: false,
    value: "",
  },
  lastModified: {
    touched: false,
    value: new Date(),
  },
};

const requiredFields = ["displayName"];

interface SetRow {
  id: string;
  displayName: string;
  description: string;
  underlayName: string;
  lastModified: Date;
}

export function SetAdmin() {
  const source = useAdminSource();
  const { studyId, underlayName } =
    useParams<{ studyId: string; underlayName: string }>();
  const [activeSet, setActiveSet] = useState<CohortV2>(emptySet);
  const [formState, setFormState] = useState(initialFormState);
  const [columnFilters, setColumnFilters] = useState({
    displayName: "",
    irbNumber: "",
  });
  const [creatingSet, setCreatingSet] = useState<boolean>(false);
  const [editingSet, setEditingSet] = useState<boolean>(false);
  const [loadingSet, setLoadingSet] = useState<boolean>(false);
  const [loadingSetList, setLoadingSetList] = useState<boolean>(true);
  const [sets, setSets] = useState<CohortV2[]>([]);

  useEffect(() => {
    getSets();
  }, []);

  const getSets = async () => {
    const sets = await source.getCohortsForStudy(studyId || "");
    setSets(sets);
    setLoadingSetList(false);
  };

  const updateSet = async () => {
    setLoadingSet(true);
    const updatedSet = await source.updateCohort(
      studyId || "",
      activeSet?.id,
      formState.displayName.value,
      formState.description.value,
      activeSet?.criteriaGroups
    );
    setActiveSet(updatedSet);
    setEditingSet(false);
    setLoadingSet(false);
    setLoadingSetList(true);
    getSets();
  };

  const createSet = async () => {
    setLoadingSet(true);
    const newSet = await source.createCohort(
      studyId || "",
      formState.displayName.value,
      formState.description.value,
      underlayName || ""
    );
    setActiveSet(newSet);
    await getSets();
    setCreatingSet(false);
    setLoadingSet(false);
  };

  const handleInputChange = (name: string, value: string) => {
    setFormState((prevState) => ({
      ...prevState,
      [name]: { value, touched: true },
    }));
  };

  const handleFilterChange = (name: string, value: string) => {
    setColumnFilters((prevState) => ({ ...prevState, [name]: value }));
  };

  const getFilteredRowsFromSets = () => {
    return sets.filter(filterSetRows);
  };

  const populateSetForm = (set: CohortV2) => {
    const newFormState = {
      displayName: {
        touched: false,
        value: set.displayName || "",
      },
      description: {
        touched: false,
        value: set.description || "",
      },
      underlayName: {
        touched: false,
        value: set.underlayName || "",
      },
      lastModified: {
        touched: false,
        value: set.lastModified || new Date(),
      },
    };
    setFormState(newFormState);
  };

  const clearSetForm = () => {
    setFormState(initialFormState);
  };

  const filterSetRows = (set: CohortV2) =>
    !columnFilters.displayName ||
    set?.displayName
      ?.toLowerCase()
      .includes(columnFilters.displayName.toLowerCase());

  const onRowSelect = (row: SetRow) => {
    const newActiveSet = sets.find((ws) => ws.id === row.id);
    const newFormState = {
      displayName: {
        touched: false,
        value: row.displayName,
      },
      description: {
        touched: false,
        value: row.description,
      },
      underlayName: {
        touched: false,
        value: row.underlayName,
      },
      lastModified: {
        touched: false,
        value: row.lastModified,
      },
    };
    setFormState(newFormState);
    if (newActiveSet) {
      setActiveSet(newActiveSet);
    }
  };

  const formIsInvalid = () =>
    Object.entries(formState).some(
      ([key, formField]) => requiredFields.includes(key) && !formField.value
    );

  return (
    <Grid container spacing={2}>
      <Grid item xs={6}>
        <Box sx={{ height: 400, width: "100%" }}>
          <DataGrid
            columns={columns(handleFilterChange)}
            rows={getFilteredRowsFromSets()}
            loading={loadingSetList}
            onRowClick={({ row }) => onRowSelect(row as SetRow)}
            hideFooter={getFilteredRowsFromSets().length <= ROWS_PER_PAGE}
            hideFooterSelectedRowCount
            disableSelectionOnClick
            pageSize={ROWS_PER_PAGE}
            rowsPerPageOptions={[]}
          />
        </Box>
      </Grid>
      <Grid item xs={6}>
        <Box sx={{ position: "relative" }}>
          <Backdrop invisible open={loadingSet} sx={{ position: "absolute" }}>
            <CircularProgress />
          </Backdrop>
          <Stack spacing={2} direction="row">
            <Button
              disabled={loadingSet}
              onClick={() => {
                clearSetForm();
                setCreatingSet(true);
              }}
              variant="outlined"
            >
              Add Set
            </Button>
            <Button
              disabled={activeSet?.id === "" || loadingSet}
              onClick={() => setEditingSet(true)}
              variant="outlined"
            >
              Edit Set
            </Button>
            <Button
              disabled={activeSet?.id === "" || loadingSet}
              variant="outlined"
            >
              Add Set Users
            </Button>
          </Stack>
          <div>
            <TextField
              sx={DISABLED_SX}
              disabled={!(creatingSet || editingSet)}
              label={"Set name"}
              name="displayName"
              fullWidth
              InputLabelProps={{
                shrink: !!formState?.displayName,
              }}
              value={formState?.displayName.value || ""}
              onChange={({ target: { name, value } }) =>
                handleInputChange(name, value)
              }
              error={
                formState.displayName.touched && !formState.displayName.value
              }
              variant={"outlined"}
            />
          </div>
          <div>
            <TextField
              sx={DISABLED_SX}
              disabled={!(creatingSet || editingSet)}
              label={"Description"}
              name="description"
              fullWidth
              InputLabelProps={{
                shrink: !!formState?.description,
              }}
              multiline
              rows={4}
              value={formState?.description.value || ""}
              onChange={({ target: { name, value } }) =>
                handleInputChange(name, value)
              }
              variant={"outlined"}
            />
          </div>
          <div>
            <TextField
              sx={DISABLED_SX}
              disabled={!(creatingSet || editingSet)}
              label={"Study Name"}
              name="studyName"
              InputLabelProps={{
                shrink: !!formState?.underlayName,
              }}
              value={formState?.underlayName.value || ""}
              onChange={({ target: { name, value } }) =>
                handleInputChange(name, value)
              }
              variant={"outlined"}
            />
          </div>
          <div></div>
          {(creatingSet || editingSet) && (
            <Stack spacing={2} direction="row">
              <Button
                disabled={loadingSet || formIsInvalid()}
                onClick={() => {
                  if (creatingSet) {
                    createSet();
                  } else {
                    updateSet();
                  }
                }}
                variant="outlined"
              >
                {creatingSet ? "Save" : "Update"} Set
              </Button>
              <Button
                disabled={loadingSet}
                onClick={() => {
                  if (creatingSet) {
                    clearSetForm();
                    setCreatingSet(false);
                  } else {
                    populateSetForm(activeSet);
                    setEditingSet(false);
                  }
                }}
                variant="outlined"
              >
                Cancel
              </Button>
            </Stack>
          )}
        </Box>
      </Grid>
    </Grid>
  );
}
