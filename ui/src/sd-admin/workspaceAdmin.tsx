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
import { CreateStudyRequest, StudyV2, UpdateStudyRequest } from "tanagra-api";

const columns = (
  filterFn: (name: string, value: string) => void
): GridColDef[] => [
  {
    field: "displayName",
    headerName: "Workspace Name",
    sortable: true,
    disableColumnMenu: true,
    flex: 1,
    renderHeader: () => (
      <div style={{ lineHeight: "1.5rem" }}>
        <div>Workspace Name</div>
        <div>
          <TextField
            sx={{
              "label+.css-apw9r9-MuiInputBase-root-MuiInput-root": {
                margin: 0,
              },
              ".css-1ktftp8-MuiFormLabel-root-MuiInputLabel-root": {
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
              "label+.css-apw9r9-MuiInputBase-root-MuiInput-root": {
                margin: 0,
              },
              ".css-1ktftp8-MuiFormLabel-root-MuiInputLabel-root": {
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

const mapWorkspaceRows = ({
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

const emptyWorkspace: StudyV2 = {
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

interface WorkspaceRow {
  id: string;
  displayName: string;
  description: string;
  irbNumber: string;
  pi: string;
}

export function WorkspaceAdmin() {
  const source = useAdminSource();
  const [activeWorkspace, setActiveWorkspace] =
    useState<StudyV2>(emptyWorkspace);
  const [formState, setFormState] = useState(initialFormState);
  const [columnFilters, setColumnFilters] = useState({
    displayName: "",
    irbNumber: "",
  });
  const [creatingWorkspace, setCreatingWorkspace] = useState<boolean>(false);
  const [editingWorkspace, setEditingWorkspace] = useState<boolean>(false);
  const [loadingWorkspace, setLoadingWorkspace] = useState<boolean>(false);
  const [loadingWorkspaceList, setLoadingWorkspaceList] =
    useState<boolean>(true);
  const [workspaces, setWorkspaces] = useState<StudyV2[]>([]);

  useEffect(() => {
    getWorkspaces();
  }, []);

  const getWorkspaces = async () => {
    const studies = await source.getStudiesList();
    setWorkspaces(studies);
    setLoadingWorkspaceList(false);
  };

  const updateWorkspace = async () => {
    setLoadingWorkspace(true);
    const updateStudyRequest: UpdateStudyRequest = {
      studyId: activeWorkspace?.id,
      studyUpdateInfoV2: {
        displayName: formState.displayName.value,
        description: formState.description.value,
      },
    };
    const updatedWorkspace = await source.updateStudy(updateStudyRequest);
    setActiveWorkspace(updatedWorkspace);
    setEditingWorkspace(false);
    setLoadingWorkspace(false);
    setLoadingWorkspaceList(true);
    getWorkspaces();
  };

  const createWorkspace = async () => {
    setLoadingWorkspace(true);
    const creatStudyRequest: CreateStudyRequest = {
      studyCreateInfoV2: {
        displayName: formState.displayName.value,
        description: formState.description.value,
        properties: [
          { key: "irbNumber", value: formState.irbNumber.value },
          { key: "pi", value: formState.pi.value },
        ],
      },
    };
    const newWorkspace = await source.createStudy(creatStudyRequest);
    setActiveWorkspace(newWorkspace);
    await getWorkspaces();
    setCreatingWorkspace(false);
    setLoadingWorkspace(false);
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
    return workspaces.filter(filterWorkspaceRows).map(mapWorkspaceRows);
  };

  const populateWorkspaceForm = (workspace: StudyV2) => {
    const newFormState = {
      displayName: {
        touched: false,
        value: workspace.displayName || "",
      },
      description: {
        touched: false,
        value: workspace.description || "",
      },
      irbNumber: {
        touched: false,
        value:
          getValueFromStudyProperty(
            workspace.properties as StudyV2Property[],
            "irbNumber"
          ) || "",
      },
      pi: {
        touched: false,
        value:
          getValueFromStudyProperty(
            workspace.properties as StudyV2Property[],
            "pi"
          ) || "",
      },
      piInput: {
        touched: false,
        value:
          getValueFromStudyProperty(
            workspace.properties as StudyV2Property[],
            "pi"
          ) || "",
      },
    };
    setFormState(newFormState);
  };

  const clearWorkspaceForm = () => {
    setFormState(initialFormState);
  };

  const filterWorkspaceRows = (workspace: StudyV2) =>
    (!columnFilters.displayName ||
      workspace?.displayName
        ?.toLowerCase()
        .includes(columnFilters.displayName.toLowerCase())) &&
    (!columnFilters.irbNumber ||
      workspace?.properties?.some((pair) => {
        const { key, value } = pair as StudyV2Property;
        return (
          key === "irbNumber" &&
          value.toLowerCase().includes(columnFilters.irbNumber.toLowerCase())
        );
      }));

  const onRowSelect = (row: WorkspaceRow) => {
    const newActiveWorkspace = workspaces.find((ws) => ws.id === row.id);
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
    if (newActiveWorkspace) {
      setActiveWorkspace(newActiveWorkspace);
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
            loading={loadingWorkspaceList}
            onRowClick={({ row }) => onRowSelect(row as WorkspaceRow)}
          />
        </Box>
      </Grid>
      <Grid item xs={6}>
        <Box sx={{ position: "relative" }}>
          <Backdrop
            invisible
            open={loadingWorkspace}
            sx={{ position: "absolute" }}
          >
            <CircularProgress />
          </Backdrop>
          <Stack spacing={2} direction="row">
            <Button
              disabled={loadingWorkspace}
              onClick={() => {
                clearWorkspaceForm();
                setCreatingWorkspace(true);
              }}
              variant="outlined"
            >
              Add Workspace
            </Button>
            <Button
              disabled={activeWorkspace?.id === "" || loadingWorkspace}
              onClick={() => setEditingWorkspace(true)}
              variant="outlined"
            >
              Edit Workspace
            </Button>
            <Button
              disabled={activeWorkspace?.id === "" || loadingWorkspace}
              variant="outlined"
            >
              Add Workspace Users
            </Button>
          </Stack>
          <div>
            <TextField
              disabled={!(creatingWorkspace || editingWorkspace)}
              label={"Workspace name"}
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
              disabled={!(creatingWorkspace || editingWorkspace)}
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
              disabled={!(creatingWorkspace || editingWorkspace)}
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
              disabled={!(creatingWorkspace || editingWorkspace)}
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
          {(creatingWorkspace || editingWorkspace) && (
            <Stack spacing={2} direction="row">
              <Button
                disabled={loadingWorkspace || formIsInvalid()}
                onClick={() => {
                  if (creatingWorkspace) {
                    createWorkspace();
                  } else {
                    updateWorkspace();
                  }
                }}
                variant="outlined"
              >
                {creatingWorkspace ? "Save" : "Update"} Workspace
              </Button>
              <Button
                disabled={loadingWorkspace}
                onClick={() => {
                  if (creatingWorkspace) {
                    clearWorkspaceForm();
                    setCreatingWorkspace(false);
                  } else {
                    populateWorkspaceForm(activeWorkspace);
                    setEditingWorkspace(false);
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
