import Autocomplete from "@mui/material/Autocomplete";
import Backdrop from "@mui/material/Backdrop";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Grid from "@mui/material/Grid";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import { useEffect, useState } from "react";
import { useAdminSource } from "sd-admin/source";
import { CohortV2, CriteriaGroupV2, StudyV2 } from "tanagra-api";

const columns = (
  filterFn: (name: string, value: string) => void
): GridColDef[] => [
  {
    field: "displayName",
    headerName: "Cohort Name",
    sortable: true,
    disableColumnMenu: true,
    flex: 1,
    renderHeader: () => (
      <div style={{ lineHeight: "1.5rem" }}>
        <div>Cohort Name</div>
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
    field: "created",
    headerName: "Create Date",
    sortable: true,
    disableColumnMenu: true,
    width: 120,
    renderHeader: () => (
      <div style={{ lineHeight: "1.5rem" }}>
        <div>Create Date</div>
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
            name="created"
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

export const mapCohortRow = (
  {
    created,
    createdBy,
    criteriaGroups,
    description,
    displayName,
    id,
    lastModified,
  }: CohortV2,
  studyName: string | undefined
) =>
  ({
    created: created.toLocaleDateString(),
    createdBy,
    criteriaGroups,
    description,
    displayName,
    id,
    lastModified: lastModified.toLocaleDateString(),
    studyName,
  } as CohortRow);

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

const emptyCohort: CohortRow = {
  id: "",
  displayName: "",
  description: "",
  studyName: "",
  criteriaGroups: [],
  created: "",
  createdBy: "",
  lastModified: "",
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
  studyName: {
    touched: false,
    value: "",
  },
  studyNameInput: {
    touched: false,
    value: "",
  },
  lastModified: {
    touched: false,
    value: "",
  },
};

const requiredFields = ["displayName", "studyName"];

export interface CohortRow {
  id: string;
  studyName: string;
  displayName: string;
  description: string;
  criteriaGroups: CriteriaGroupV2[];
  created: string;
  createdBy: string;
  lastModified: string;
}

export function CohortAdmin() {
  const source = useAdminSource();
  const [activeCohort, setActiveCohort] = useState<CohortRow>(emptyCohort);
  const [formState, setFormState] = useState(initialFormState);
  const [columnFilters, setColumnFilters] = useState({
    created: "",
    displayName: "",
    studyName: "",
  });
  const [creatingCohort, setCreatingCohort] = useState<boolean>(false);
  const [editingCohort, setEditingCohort] = useState<boolean>(false);
  const [loadingCohort, setLoadingCohort] = useState<boolean>(false);
  const [loadingCohortList, setLoadingCohortList] = useState<boolean>(true);
  const [cohorts, setCohorts] = useState<CohortRow[]>([]);
  const [studies, setStudies] = useState<StudyV2[]>([]);

  useEffect(() => {
    getCohorts();
  }, []);

  const getCohorts = async () => {
    const studiesResp = await source.getStudiesList();
    setStudies(studiesResp);
    if (studiesResp.length > 0) {
      // Get cohorts for each study then consolidate into a single list
      const cohortsResp = await Promise.all(
        studiesResp.map(({ id }) => source.getCohortsForStudy(id))
      );
      const cohortsList: CohortRow[] = cohortsResp.reduce(
        (cohortList, currentResp, index) => [
          ...cohortList,
          ...currentResp.map((cohort) =>
            mapCohortRow(cohort, studiesResp[index].displayName)
          ),
        ],
        [] as CohortRow[]
      );
      setCohorts(cohortsList);
    }
    setLoadingCohortList(false);
  };

  const updateCohort = async () => {
    setLoadingCohort(true);
    const cohortStudy = studies.find(
      (study) => study.displayName === formState.studyName.value
    );
    if (cohortStudy) {
      const updatedCohort = await source.updateCohort(
        cohortStudy.id,
        activeCohort.id,
        formState.displayName.value,
        formState.description.value,
        activeCohort.criteriaGroups
      );
      setActiveCohort(mapCohortRow(updatedCohort, cohortStudy.displayName));
      setLoadingCohortList(true);
      getCohorts();
      setEditingCohort(false);
    }
    setLoadingCohort(false);
  };

  const createCohort = async () => {
    setLoadingCohort(true);
    const cohortStudy = studies.find(
      (study) => study.displayName === formState.studyName.value
    );
    if (cohortStudy) {
      const newCohort = await source.createCohort(
        cohortStudy.id,
        formState.displayName.value,
        formState.description.value,
        "aou_synthetic"
      );
      setActiveCohort(mapCohortRow(newCohort, cohortStudy.displayName));
      await getCohorts();
      setCreatingCohort(false);
    }
    setLoadingCohort(false);
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

  const getFilteredRowsFromCohorts = () => {
    return cohorts.filter(filterCohortRows);
  };

  const populateCohortForm = (cohort: CohortRow) => {
    const newFormState = {
      displayName: {
        touched: false,
        value: cohort.displayName,
      },
      description: {
        touched: false,
        value: cohort.description,
      },
      studyName: {
        touched: false,
        value: cohort.studyName,
      },
      studyNameInput: {
        touched: false,
        value: cohort.studyName,
      },
      lastModified: {
        touched: false,
        value: cohort.lastModified,
      },
    };
    setFormState(newFormState);
  };

  const clearCohortForm = () => {
    setFormState(initialFormState);
  };

  const filterCohortRows = (cohort: CohortRow) =>
    (!columnFilters.displayName ||
      cohort.displayName
        .toLowerCase()
        .includes(columnFilters.displayName.toLowerCase().trim())) &&
    (!columnFilters.created ||
      cohort.created
        .toLowerCase()
        .includes(columnFilters.created.toLowerCase().trim())) &&
    (!columnFilters.studyName ||
      cohort.studyName
        .toLowerCase()
        .includes(columnFilters.studyName.toLowerCase().trim()));

  const onRowSelect = (row: CohortRow) => {
    if (!(creatingCohort || editingCohort)) {
      const newActiveCohort = cohorts.find((ws) => ws.id === row.id);
      const newFormState = {
        displayName: {
          touched: false,
          value: row.displayName,
        },
        description: {
          touched: false,
          value: row.description,
        },
        studyName: {
          touched: false,
          value: row.studyName,
        },
        studyNameInput: {
          touched: false,
          value: row.studyName,
        },
        lastModified: {
          touched: false,
          value: row.lastModified,
        },
      };
      setFormState(newFormState);
      if (newActiveCohort) {
        setActiveCohort(newActiveCohort);
      }
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
            rows={getFilteredRowsFromCohorts()}
            loading={loadingCohortList}
            onRowClick={({ row }) => onRowSelect(row as CohortRow)}
            hideFooter={getFilteredRowsFromCohorts().length <= ROWS_PER_PAGE}
            hideFooterSelectedRowCount
            disableSelectionOnClick
            pageSize={ROWS_PER_PAGE}
            rowsPerPageOptions={[]}
          />
        </Box>
      </Grid>
      <Grid item xs={6}>
        <Box sx={{ position: "relative" }}>
          <Backdrop
            invisible
            open={loadingCohort}
            sx={{ position: "absolute" }}
          >
            <CircularProgress />
          </Backdrop>
          <Stack spacing={2} direction="row">
            <Button
              disabled={creatingCohort || editingCohort || loadingCohort}
              onClick={() => {
                clearCohortForm();
                setCreatingCohort(true);
              }}
              variant="outlined"
            >
              Add Cohort
            </Button>
            <Button
              disabled={
                activeCohort.id === "" || creatingCohort || loadingCohort
              }
              onClick={() => setEditingCohort(true)}
              variant="outlined"
            >
              Edit Cohort
            </Button>
          </Stack>
          <div>
            <TextField
              sx={DISABLED_SX}
              disabled={!(creatingCohort || editingCohort)}
              label={"Cohort name"}
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
              disabled={!(creatingCohort || editingCohort)}
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
            <Autocomplete
              sx={DISABLED_SX}
              disabled={!(creatingCohort || editingCohort)}
              options={studies.map(({ displayName }) => displayName)}
              value={formState?.studyName.value}
              onChange={(event, newValue) =>
                handleInputChange("studyName", newValue || "")
              }
              inputValue={formState?.studyNameInput.value || ""}
              onInputChange={(event, newInputValue) =>
                handleInputChange("studyNameInput", newInputValue)
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={"Study Name"}
                  name="studyName"
                  fullWidth
                  InputLabelProps={{
                    shrink: !!formState?.studyName,
                  }}
                  onChange={({ target: { name, value } }) =>
                    handleInputChange(name, value)
                  }
                  error={
                    formState.studyName.touched && !formState.studyName.value
                  }
                  variant={"outlined"}
                />
              )}
            />
          </div>
          {(creatingCohort || editingCohort) && (
            <Stack spacing={2} direction="row">
              <Button
                disabled={loadingCohort || formIsInvalid()}
                onClick={() => {
                  if (creatingCohort) {
                    createCohort();
                  } else {
                    updateCohort();
                  }
                }}
                variant="outlined"
              >
                {creatingCohort ? "Save" : "Update"} Cohort
              </Button>
              <Button
                disabled={loadingCohort}
                onClick={() => {
                  populateCohortForm(activeCohort);
                  if (creatingCohort) {
                    setCreatingCohort(false);
                  } else {
                    setEditingCohort(false);
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
