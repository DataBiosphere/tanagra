import Backdrop from "@mui/material/Backdrop";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import Collapse from "@mui/material/Collapse";
import Grid from "@mui/material/Grid";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import { useEffect, useState } from "react";
import { useAdminSource } from "sd-admin/source";
import {
  CohortV2,
  CriteriaGroupSectionV3OperatorEnum,
  StudyV2,
} from "tanagra-api";
import { CohortRow, mapCohortRow } from "./cohortAdmin";

// TODO dolbeew: may move the study table and functions to a separate component
//  in a follow-up pr since everything is duplicated in studyAdmin.tsx
const studyColumns = (
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

const cohortColumns = [
  {
    field: "id",
    headerName: "Cohort Id",
  },
  {
    field: "displayName",
    headerName: "Cohort Name",
  },
  {
    field: "createdBy",
    headerName: "Owner",
  },
  {
    field: "created",
    headerName: "Created",
  },
];
// Temp mocked cohort to test the expandable rows that list cohort criteria
const mockCohort: CohortV2 = {
  id: "f8YLRL8t",
  displayName: "Mocked cohort criteria test",
  underlayName: "verily_aou_synthetic",
  lastModified: new Date(),
  created: new Date(),
  createdBy: "test user",
  criteriaGroupSections: [
    {
      id: "n3jTaIjK",
      displayName: "Group 1",
      operator: CriteriaGroupSectionV3OperatorEnum.And,
      excluded: false,
      criteriaGroups: [
        {
          id: "ZMiJsSL8",
          displayName: "",
          entity: "",
          criteria: [
            {
              id: "ZMiJsSL8",
              displayName: "Condition: Disorder of body system",
              pluginName: "",
              selectionData: "",
              uiConfig: "",
            },
          ],
        },
        {
          id: "GkWUoXtT",
          displayName: "",
          entity: "",
          criteria: [
            {
              id: "GkWUoXtT",
              displayName:
                "Condition: Traumatic and/or non-traumatic injury of anatomical site",
              pluginName: "",
              selectionData: "",
              uiConfig: "",
            },
          ],
        },
        {
          id: "Rto3T4r6",
          displayName: "",
          entity: "",
          criteria: [
            {
              id: "Rto3T4r6",
              displayName: "Condition: Neoplasm by body site",
              pluginName: "",
              selectionData: "",
              uiConfig: "",
            },
          ],
        },
      ],
    },
    {
      id: "iZM84IuY",
      displayName: "Group 2",
      operator: CriteriaGroupSectionV3OperatorEnum.Or,
      excluded: false,
      criteriaGroups: [
        {
          id: "zyX8UoO8",
          displayName: "",
          entity: "",
          criteria: [
            {
              id: "zyX8UoO8",
              displayName: "Ethnicity: Not Hispanic or Latino",
              pluginName: "",
              selectionData: "",
              uiConfig: "",
            },
          ],
        },
      ],
    },
    {
      id: "CPYKJ3yH",
      displayName: "Group 2",
      operator: CriteriaGroupSectionV3OperatorEnum.Or,
      excluded: true,
      criteriaGroups: [
        {
          id: "lxYjNNH3",
          displayName: "",
          entity: "",
          criteria: [
            {
              id: "lxYjNNH3",
              displayName: "Sex assigned at birth: Male",
              pluginName: "",
              selectionData: "",
              uiConfig: "",
            },
          ],
        },
      ],
    },
  ],
};

function CohortHeader(props: {
  field: string;
  headerName: string;
  filterFn: (name: string, value: string) => void;
}) {
  const { field, headerName, filterFn } = props;
  return (
    <div style={{ lineHeight: "1.5rem" }}>
      <div>{headerName}</div>
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
          name={field}
          onChange={({ target: { name, value } }) => filterFn(name, value)}
          label="Filter"
          variant="standard"
          size="small"
          margin="none"
        />
      </div>
    </div>
  );
}

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
const ROWS_PER_PAGE = 25;

const emptyStudy: StudyV2 = {
  id: "",
  displayName: "",
  description: "",
  properties: [
    { key: "irbNumber", value: "" },
    { key: "pi", value: "" },
  ],
  created: new Date(),
  createdBy: "user@gmail.com",
  lastModified: new Date(),
};

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

function CohortTableRow(props: { cohortRow: CohortRow }) {
  const {
    cohortRow: { id, created, createdBy, criteriaGroupSections, displayName },
  } = props;
  const [open, setOpen] = useState(false);
  const closedStyle = {
    border: 0,
    paddingBottom: 0,
    paddingTop: 0,
  };
  return (
    <>
      <TableRow
        sx={{ cursor: "pointer", height: "2.5rem" }}
        onClick={() => setOpen(!open)}
      >
        <TableCell>{id}</TableCell>
        <TableCell>{displayName}</TableCell>
        <TableCell>{createdBy}</TableCell>
        <TableCell>{created}</TableCell>
      </TableRow>
      <TableRow>
        <TableCell style={!open ? closedStyle : {}} colSpan={4}>
          <Collapse in={open}>
            {criteriaGroupSections.length === 0 && (
              <Typography variant="body2">
                Cohort &quot;{displayName}&quot; has no criteria groups
              </Typography>
            )}
            {criteriaGroupSections.map(
              ({
                excluded,
                operator,
                criteriaGroups: [{ id: gid, criteria, displayName: groupName }],
              }) => (
                <div key={gid}>
                  <Typography variant="h6">
                    {excluded ? "Excludes" : "Includes"}{" "}
                    {operator === CriteriaGroupSectionV3OperatorEnum.Or
                      ? "any of"
                      : "all of"}
                  </Typography>
                  {criteria.length === 0 && (
                    <Typography variant="body2">
                      Group &quot;{groupName}&quot; has no criteria
                    </Typography>
                  )}
                  {criteria.map(({ id: cid, displayName: criteriaName }) => (
                    <Typography key={cid} variant="body1">
                      {criteriaName}
                    </Typography>
                  ))}
                </div>
              )
            )}
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}

export function CohortAudit() {
  const source = useAdminSource();
  const [activeStudy, setActiveStudy] = useState<StudyV2>(emptyStudy);
  const [cohortFilters, setCohortFilters] = useState({
    id: "",
    displayName: "",
    createdBy: "",
    created: "",
  });
  const [studyFilters, setStudyFilters] = useState({
    displayName: "",
    irbNumber: "",
  });
  const [loadingStudyCohorts, setLoadingStudyCohorts] =
    useState<boolean>(false);
  const [loadingStudyList, setLoadingStudyList] = useState<boolean>(true);
  const [studies, setStudies] = useState<StudyV2[]>([]);
  const [studyCohorts, setStudyCohorts] = useState<CohortRow[]>([]);

  useEffect(() => {
    getStudies();
  }, []);

  useEffect(() => {
    if (activeStudy.id) {
      getStudyCohorts();
    }
  }, [activeStudy]);

  const getStudies = async () => {
    const studies = await source.getStudiesList();
    setStudies(studies);
    setLoadingStudyList(false);
  };

  const getStudyCohorts = async () => {
    setLoadingStudyCohorts(true);
    const cohorts = await source.getCohortsForStudy(activeStudy.id);
    // Add mock cohort since cohorts returned from endpoint currently have no criteria groups
    cohorts.push(mockCohort);
    setStudyCohorts(
      cohorts.map((cohort) => mapCohortRow(cohort, activeStudy.displayName))
    );
    setLoadingStudyCohorts(false);
  };

  const handleStudyFilterChange = (name: string, value: string) => {
    setStudyFilters((prevState) => ({ ...prevState, [name]: value }));
  };

  const handleCohortFilterChange = (name: string, value: string) => {
    setCohortFilters((prevState) => ({ ...prevState, [name]: value }));
  };

  const getFilteredRowsFromStudies = () => {
    return studies.filter(filterStudyRows).map(mapStudyRows);
  };

  const getFilteredRowsFromCohorts = () => {
    return studyCohorts.filter(filterCohortRows);
  };

  const filterStudyRows = (study: StudyV2) =>
    (!studyFilters.displayName ||
      study?.displayName
        ?.toLowerCase()
        .includes(studyFilters.displayName.toLowerCase())) &&
    (!studyFilters.irbNumber ||
      study?.properties?.some((pair) => {
        const { key, value } = pair as StudyV2Property;
        return (
          key === "irbNumber" &&
          value.toLowerCase().includes(studyFilters.irbNumber.toLowerCase())
        );
      }));

  const filterCohortRows = (cohort: CohortRow) =>
    (!cohortFilters.id ||
      cohort?.id?.toLowerCase().includes(cohortFilters.id.toLowerCase())) &&
    (!cohortFilters.displayName ||
      cohort?.displayName
        ?.toLowerCase()
        .includes(cohortFilters.displayName.toLowerCase())) &&
    (!cohortFilters.createdBy ||
      cohort?.createdBy
        ?.toLowerCase()
        .includes(cohortFilters.createdBy.toLowerCase())) &&
    (!cohortFilters.created ||
      cohort?.created
        ?.toLowerCase()
        .includes(cohortFilters.created.toLowerCase()));

  const onStudySelect = (row: StudyRow) => {
    const newActiveStudy = studies.find((ws) => ws.id === row.id);
    if (newActiveStudy) {
      setActiveStudy(newActiveStudy);
    }
  };

  return (
    <Grid container spacing={2}>
      <Grid item xs={6}>
        <Box sx={{ height: 400, width: "100%" }}>
          <DataGrid
            columns={studyColumns(handleStudyFilterChange)}
            rows={getFilteredRowsFromStudies()}
            loading={loadingStudyList}
            onRowClick={({ row }) => onStudySelect(row as StudyRow)}
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
          <Backdrop
            invisible
            open={loadingStudyCohorts}
            sx={{ position: "absolute" }}
          >
            <CircularProgress />
          </Backdrop>
          <TableContainer>
            <Table aria-label="collapsible table">
              <TableHead>
                <TableRow>
                  {cohortColumns.map(({ field, headerName }, index) => (
                    <TableCell key={index}>
                      <CohortHeader
                        field={field}
                        headerName={headerName}
                        filterFn={handleCohortFilterChange}
                      />
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {getFilteredRowsFromCohorts().map((cohort) => (
                  <CohortTableRow key={cohort.id} cohortRow={cohort} />
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      </Grid>
    </Grid>
  );
}
