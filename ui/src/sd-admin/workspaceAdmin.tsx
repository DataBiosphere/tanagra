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

const columns = (
  filterFn: (name: string, value: string) => void
): GridColDef[] => [
  {
    field: "workspaceName",
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
            name="workspaceName"
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

interface MockWorkspace {
  id: number;
  workspaceName: string;
  description: string;
  irbNumber: number;
  pi: string;
}
let mockWorkspaces: MockWorkspace[] = [
  {
    id: 1,
    workspaceName:
      "A case-control AQP11 gene association study in the context to toxic AKI patients",
    description: "Mock workspace 1 for testing SD Admin",
    irbNumber: 91526,
    pi: "Will",
  },
  {
    id: 2,
    workspaceName:
      "A retrospective analysis to determine diagnostic markers of RYGB failure",
    description: "Mock workspace 2 for testing SD Admin",
    irbNumber: 130747,
    pi: "Erik",
  },
  {
    id: 3,
    workspaceName: "Risk Factors for Enteral Access Complications",
    description: "Mock workspace 3 for testing SD Admin",
    irbNumber: 91571,
    pi: "Tim",
  },
  {
    id: 4,
    workspaceName: "Exploration of Neurological Atlas Development",
    description: "Mock workspace 4 for testing SD Admin",
    irbNumber: 91193,
    pi: "Brian",
  },
  {
    id: 5,
    workspaceName:
      "Genetic determinants of hypothyroidism and uncontrolled hypertension:  Electronic phenotype detection",
    description: "Mock workspace 5 for testing SD Admin",
    irbNumber: 91078,
    pi: "Chenchal",
  },
  {
    id: 6,
    workspaceName:
      "Magnesium deficiency among patients with chronic and acute diseases",
    description: "Mock workspace 6 for testing SD Admin",
    irbNumber: 91221,
    pi: "Will",
  },
  {
    id: 7,
    workspaceName: "Genetic Polymorphisms and Pain",
    description: "Mock workspace 7 for testing SD Admin",
    irbNumber: 90968,
    pi: "Erik",
  },
  {
    id: 8,
    workspaceName:
      "VESPA - Vanderbilt Electronic Systems for Pharmacogenomic Assessment",
    description: "Mock workspace 8 for testing SD Admin",
    irbNumber: 90945,
    pi: "Tim",
  },
  {
    id: 9,
    workspaceName:
      "Evaluation of Natural Language Processing Software in De-identified Electronic Medical Records",
    description: "Mock workspace 9 for testing SD Admin",
    irbNumber: 91193,
    pi: "Brian",
  },
  {
    id: 10,
    workspaceName:
      "Evaluation of Sources, Frequency, and Various Methods of Removal of PHI Within Vanderbilt EMRs",
    description: "Mock workspace 10 for testing SD Admin",
    irbNumber: 56789,
    pi: "Chenchal",
  },
];
const piOptions = ["", "Will", "Erik", "Tim", "Brian", "Chenchal"];

function getMockWorkspaces(): Promise<MockWorkspace[]> {
  return new Promise<MockWorkspace[]>((resolve) => resolve(mockWorkspaces));
}
function getMockWorkspace(workspaceId: number): Promise<MockWorkspace> {
  return new Promise<MockWorkspace>((resolve) => {
    const mockWorkspace =
      mockWorkspaces.find(({ id }) => id === workspaceId) ||
      ({} as MockWorkspace);
    resolve(mockWorkspace);
  });
}
function updateMockWorkspace(
  updatedWorkspace: MockWorkspace
): Promise<MockWorkspace> {
  return new Promise<MockWorkspace>((resolve) => {
    mockWorkspaces = mockWorkspaces.map((mockWs) =>
      mockWs.id === updatedWorkspace.id ? updatedWorkspace : mockWs
    );
    resolve(updatedWorkspace);
  });
}
function createMockWorkspace(workspace: MockWorkspace): Promise<MockWorkspace> {
  return new Promise<MockWorkspace>((resolve) => {
    if (workspace.id === -1) {
      workspace.id =
        mockWorkspaces.reduce((max, ws) => Math.max(max, ws.id), 0) + 1;
    }
    mockWorkspaces.push(workspace);
    resolve(workspace);
  });
}

const emptyWorkspace: MockWorkspace = {
  id: -1,
  workspaceName: "",
  description: "",
  irbNumber: -1,
  pi: "",
};

export function WorkspaceAdmin() {
  const [activeWorkspace, setActiveWorkspace] =
    useState<MockWorkspace>(emptyWorkspace);
  const [formState, setFormState] = useState({
    workspaceName: "",
    description: "",
    irbNumber: "",
    pi: "",
    piInput: "",
  });
  const [columnFilters, setColumnFilters] = useState({
    workspaceName: "",
    irbNumber: "",
  });
  const [creatingWorkspace, setCreatingWorkspace] = useState<boolean>(false);
  const [editingWorkspace, setEditingWorkspace] = useState<boolean>(false);
  const [loadingWorkspace, setLoadingWorkspace] = useState<boolean>(false);
  const [loadingWorkspaceList, setLoadingWorkspaceList] =
    useState<boolean>(true);
  const [workspaces, setWorkspaces] = useState<MockWorkspace[]>([]);

  const getWorkspaces = async () => {
    await setTimeout(async () => {
      const workspacesResponse = await getMockWorkspaces();
      setWorkspaces(workspacesResponse);
      setLoadingWorkspaceList(false);
    }, 1000);
  };

  const getWorkspace = async (id: number) => {
    setLoadingWorkspace(true);
    await setTimeout(async () => {
      const workspaceResponse = await getMockWorkspace(id);
      populateWorkspaceForm(workspaceResponse);
      setActiveWorkspace(workspaceResponse);
      setLoadingWorkspace(false);
    }, 1000);
  };

  const updateWorkspace = async () => {
    setLoadingWorkspace(true);
    await setTimeout(async () => {
      const updatedWorkspace = {
        id: activeWorkspace?.id || -1,
        workspaceName: formState.workspaceName,
        description: formState.description,
        irbNumber: +formState.irbNumber,
        pi: formState.pi,
      };
      const updatedResponse = await updateMockWorkspace(updatedWorkspace);
      setActiveWorkspace(updatedResponse);
      setWorkspaces((prevState) =>
        prevState.map((ws) =>
          ws.id === updatedResponse.id ? updatedResponse : ws
        )
      );
      setEditingWorkspace(false);
      setLoadingWorkspace(false);
    }, 1000);
  };

  const createWorkspace = async () => {
    setLoadingWorkspace(true);
    await setTimeout(async () => {
      const newWorkspace = {
        id: -1,
        workspaceName: formState.workspaceName,
        description: formState.description,
        irbNumber: +formState.irbNumber,
        pi: formState.pi,
      };
      const createdResponse = await createMockWorkspace(newWorkspace);
      setActiveWorkspace(createdResponse);
      await getWorkspaces();
      setCreatingWorkspace(false);
      setLoadingWorkspace(false);
    }, 1000);
  };

  const handleInputChange = (name: string, value: string) => {
    setFormState((prevState) => ({ ...prevState, [name]: value }));
  };

  const handleFilterChange = (name: string, value: string) => {
    setColumnFilters((prevState) => ({ ...prevState, [name]: value }));
  };

  const populateWorkspaceForm = (workspace: MockWorkspace) => {
    const newFormState = {
      workspaceName: workspace.workspaceName,
      description: workspace.description,
      irbNumber: workspace.irbNumber.toString(),
      pi: workspace.pi,
      piInput: workspace.pi,
    };
    setFormState(newFormState);
  };

  const clearWorkspaceForm = () => {
    setFormState({
      workspaceName: "",
      description: "",
      irbNumber: "",
      pi: "",
      piInput: "",
    });
  };

  const filterWorkspaceRows = (workspace: MockWorkspace) =>
    (!columnFilters.workspaceName ||
      workspace.workspaceName
        .toLowerCase()
        .includes(columnFilters.workspaceName.toLowerCase())) &&
    (!columnFilters.irbNumber ||
      workspace.irbNumber
        .toString()
        .toLowerCase()
        .includes(columnFilters.irbNumber.toLowerCase()));

  useEffect(() => {
    getWorkspaces();
  }, []);

  return (
    <Grid container spacing={2}>
      <Grid item xs={6}>
        <Box sx={{ height: 400, width: "100%" }}>
          <DataGrid
            columns={columns(handleFilterChange)}
            rows={workspaces.filter(filterWorkspaceRows)}
            loading={loadingWorkspaceList}
            onRowClick={({ row }) => getWorkspace(row.id)}
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
              disabled={activeWorkspace?.id < 0 || loadingWorkspace}
              onClick={() => setEditingWorkspace(true)}
              variant="outlined"
            >
              Edit Workspace
            </Button>
            <Button
              disabled={activeWorkspace?.id < 0 || loadingWorkspace}
              variant="outlined"
            >
              Add Workspace Users
            </Button>
          </Stack>
          <div>
            <TextField
              disabled={!(creatingWorkspace || editingWorkspace)}
              label={"Workspace name"}
              name="workspaceName"
              fullWidth
              InputLabelProps={{
                shrink: !!formState?.workspaceName,
              }}
              value={formState?.workspaceName || ""}
              onChange={({ target: { name, value } }) =>
                handleInputChange(name, value)
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
              value={formState?.description || ""}
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
              value={formState?.irbNumber || ""}
              onChange={({ target: { name, value } }) =>
                handleInputChange(name, value)
              }
              variant={"outlined"}
            />
          </div>
          <div>
            <Autocomplete
              disabled={!(creatingWorkspace || editingWorkspace)}
              options={piOptions}
              value={formState?.pi}
              onChange={(event, newValue) =>
                handleInputChange("pi", newValue || "")
              }
              inputValue={formState?.piInput || ""}
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
                  variant={"outlined"}
                />
              )}
            />
          </div>
          {(creatingWorkspace || editingWorkspace) && (
            <Stack spacing={2} direction="row">
              <Button
                disabled={loadingWorkspace}
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
