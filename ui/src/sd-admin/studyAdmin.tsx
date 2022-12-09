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
import { StudyV2 } from "tanagra-api";

const columns = (
  filterFn: (name: string, value: string) => void
): GridColDef[] => [
  {
    field: "displayName",
    headerName: "Study Name",
    sortable: true,
    disableColumnMenu: true,
    flex: 1,
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
    field: "irbNumber",
    headerName: "IRB Number",
    sortable: true,
    disableColumnMenu: true,
    width: 120,
    renderHeader: () => (
      <div style={{ lineHeight: "1.5rem" }}>
        <div>IRB Number</div>
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
            name="irbNumber"
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

// Temp PI options until endpoint in place
const piOptions = ["", "Will", "Erik", "Tim", "Brian", "Chenchal"];

const getValueFromStudyProperty = (
  properties: StudyV2Property[],
  key: string
) => properties?.find((pair) => pair.key === key)?.value;

const mapStudyRows = ({
  id,
  displayName,
  description,
  properties,
}: StudyV2) => ({
  id,
  displayName,
  description,
  irbNumber: getValueFromStudyProperty(
    properties as StudyV2Property[],
    "irbNumber"
  ),
  pi: getValueFromStudyProperty(properties as StudyV2Property[], "pi"),
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

const emptyStudy: StudyV2 = {
  id: "",
  displayName: "",
  description: "",
  properties: [
    { key: "irbNumber", value: "" },
    { key: "pi", value: "" },
  ],
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
  irbNumber: {
    touched: false,
    value: "",
  },
  pi: {
    touched: false,
    value: "",
  },
  piInput: {
    touched: false,
    value: "",
  },
};

const requiredFields = ["displayName", "irbNumber", "pi"];

interface StudyV2Property {
  key: string;
  value: string;
}

interface StudyRow {
  id: string;
  displayName: string;
  description: string;
  irbNumber: string;
  pi: string;
}

export function StudyAdmin() {
  const source = useAdminSource();
  const [activeStudy, setActiveStudy] = useState<StudyV2>(emptyStudy);
  const [formState, setFormState] = useState(initialFormState);
  const [columnFilters, setColumnFilters] = useState({
    displayName: "",
    irbNumber: "",
  });
  const [creatingStudy, setCreatingStudy] = useState<boolean>(false);
  const [editingStudy, setEditingStudy] = useState<boolean>(false);
  const [loadingStudy, setLoadingStudy] = useState<boolean>(false);
  const [loadingStudyList, setLoadingStudyList] = useState<boolean>(true);
  const [studies, setStudies] = useState<StudyV2[]>([]);

  useEffect(() => {
    getStudies();
  }, []);

  const getStudies = async () => {
    const studies = await source.getStudiesList();
    setStudies(studies);
    setLoadingStudyList(false);
  };

  const updateStudy = async () => {
    setLoadingStudy(true);
    const updatedStudy = await source.updateStudy(
      activeStudy?.id,
      formState.displayName.value,
      formState.description.value
    );
    setActiveStudy(updatedStudy);
    setEditingStudy(false);
    setLoadingStudy(false);
    setLoadingStudyList(true);
    getStudies();
  };

  const createStudy = async () => {
    setLoadingStudy(true);
    const newStudy = await source.createStudy(
      formState.displayName.value,
      formState.description.value,
      [
        { key: "irbNumber", value: formState.irbNumber.value },
        { key: "pi", value: formState.pi.value },
      ]
    );
    setActiveStudy(newStudy);
    await getStudies();
    setCreatingStudy(false);
    setLoadingStudy(false);
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

  const getFilteredRowsFromStudies = () => {
    return studies.filter(filterStudyRows).map(mapStudyRows);
  };

  const populateStudyForm = (study: StudyV2) => {
    const newFormState = {
      displayName: {
        touched: false,
        value: study.displayName || "",
      },
      description: {
        touched: false,
        value: study.description || "",
      },
      irbNumber: {
        touched: false,
        value:
          getValueFromStudyProperty(
            study.properties as StudyV2Property[],
            "irbNumber"
          ) || "",
      },
      pi: {
        touched: false,
        value:
          getValueFromStudyProperty(
            study.properties as StudyV2Property[],
            "pi"
          ) || "",
      },
      piInput: {
        touched: false,
        value:
          getValueFromStudyProperty(
            study.properties as StudyV2Property[],
            "pi"
          ) || "",
      },
    };
    setFormState(newFormState);
  };

  const clearStudyForm = () => {
    setFormState(initialFormState);
  };

  const filterStudyRows = (study: StudyV2) =>
    (!columnFilters.displayName ||
      study?.displayName
        ?.toLowerCase()
        .includes(columnFilters.displayName.toLowerCase())) &&
    (!columnFilters.irbNumber ||
      study?.properties?.some((pair) => {
        const { key, value } = pair as StudyV2Property;
        return (
          key === "irbNumber" &&
          value.toLowerCase().includes(columnFilters.irbNumber.toLowerCase())
        );
      }));

  const onRowSelect = (row: StudyRow) => {
    const newActiveStudy = studies.find((ws) => ws.id === row.id);
    const newFormState = {
      displayName: {
        touched: false,
        value: row.displayName,
      },
      description: {
        touched: false,
        value: row.description,
      },
      irbNumber: {
        touched: false,
        value: row.irbNumber,
      },
      pi: {
        touched: false,
        value: row.pi,
      },
      piInput: {
        touched: false,
        value: row.pi,
      },
    };
    setFormState(newFormState);
    if (newActiveStudy) {
      setActiveStudy(newActiveStudy);
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
            rows={getFilteredRowsFromStudies()}
            loading={loadingStudyList}
            onRowClick={({ row }) => onRowSelect(row as StudyRow)}
            hideFooter={getFilteredRowsFromStudies().length <= ROWS_PER_PAGE}
            hideFooterSelectedRowCount
            disableSelectionOnClick
            pageSize={ROWS_PER_PAGE}
            rowsPerPageOptions={[]}
          />
        </Box>
      </Grid>
      <Grid item xs={6}>
        <Box sx={{ position: "relative" }}>
          <Backdrop invisible open={loadingStudy} sx={{ position: "absolute" }}>
            <CircularProgress />
          </Backdrop>
          <Stack spacing={2} direction="row">
            <Button
              disabled={loadingStudy}
              onClick={() => {
                clearStudyForm();
                setCreatingStudy(true);
              }}
              variant="outlined"
            >
              Add Study
            </Button>
            <Button
              disabled={activeStudy?.id === "" || loadingStudy}
              onClick={() => setEditingStudy(true)}
              variant="outlined"
            >
              Edit Study
            </Button>
            <Button
              disabled={activeStudy?.id === "" || loadingStudy}
              variant="outlined"
            >
              Add Study Users
            </Button>
          </Stack>
          <div>
            <TextField
              sx={DISABLED_SX}
              disabled={!(creatingStudy || editingStudy)}
              label={"Study name"}
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
              disabled={!(creatingStudy || editingStudy)}
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
              disabled={!(creatingStudy || editingStudy)}
              label={"IRB Number"}
              name="irbNumber"
              InputLabelProps={{
                shrink: !!formState?.irbNumber,
              }}
              value={formState?.irbNumber.value || ""}
              onChange={({ target: { name, value } }) =>
                handleInputChange(name, value)
              }
              error={formState.irbNumber.touched && !formState.irbNumber.value}
              variant={"outlined"}
            />
          </div>
          <div>
            <Autocomplete
              sx={DISABLED_SX}
              disabled={!(creatingStudy || editingStudy)}
              options={piOptions}
              value={formState?.pi.value}
              onChange={(event, newValue) =>
                handleInputChange("pi", newValue || "")
              }
              inputValue={formState?.piInput.value || ""}
              onInputChange={(event, newInputValue) =>
                handleInputChange("piInput", newInputValue)
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={"PI"}
                  name="pi"
                  fullWidth
                  InputLabelProps={{
                    shrink: !!formState?.pi,
                  }}
                  onChange={({ target: { name, value } }) =>
                    handleInputChange(name, value)
                  }
                  error={formState.pi.touched && !formState.pi.value}
                  variant={"outlined"}
                />
              )}
            />
          </div>
          {(creatingStudy || editingStudy) && (
            <Stack spacing={2} direction="row">
              <Button
                disabled={loadingStudy || formIsInvalid()}
                onClick={() => {
                  if (creatingStudy) {
                    createStudy();
                  } else {
                    updateStudy();
                  }
                }}
                variant="outlined"
              >
                {creatingStudy ? "Save" : "Update"} Study
              </Button>
              <Button
                disabled={loadingStudy}
                onClick={() => {
                  if (creatingStudy) {
                    clearStudyForm();
                    setCreatingStudy(false);
                  } else {
                    populateStudyForm(activeStudy);
                    setEditingStudy(false);
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
