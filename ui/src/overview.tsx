import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import Accordion from "@mui/material/Accordion";
import AccordionDetails from "@mui/material/AccordionDetails";
import AccordionSummary from "@mui/material/AccordionSummary";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { EntityCountsApiContext } from "apiContext";
import {
  deleteCriteria,
  deleteGroup,
  insertCriteria,
  insertGroup,
  renameCriteria,
  renameGroup,
} from "cohortsSlice";
import Loading from "components/loading";
import { useMenu } from "components/menu";
import { useTextInputDialog } from "components/textInputDialog";
import { useSource } from "data/source";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useCohort, useUnderlay } from "hooks";
import { useCallback, useContext } from "react";
import { Link as RouterLink, useHistory } from "react-router-dom";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { createUrl } from "router";
import * as tanagra from "tanagra-api";
import {
  createCriteria,
  generateQueryFilter,
  getCriteriaPlugin,
} from "./cohort";

export default function Overview() {
  const cohort = useCohort();

  return (
    <>
      <ActionBar title={cohort.name} />
      <Grid container columns={3} columnSpacing={5} className="overview">
        <ParticipantsSelector kind={tanagra.GroupKindEnum.Included} />
        <ParticipantsSelector kind={tanagra.GroupKindEnum.Excluded} />
        <DemographicCharts cohort={cohort} />
      </Grid>
    </>
  );
}

function ParticipantsSelector(props: { kind?: tanagra.GroupKindEnum }) {
  const cohort = useCohort();

  return (
    <Grid item xs={1}>
      <Typography variant="h4">
        {props.kind === tanagra.GroupKindEnum.Included
          ? "Included Participants"
          : "Excluded Participants"}
      </Typography>
      <Stack spacing={0}>
        {cohort.groups
          .filter((g) => g.kind === props.kind)
          .map((group, index) => (
            <Box key={group.id}>
              <ParticipantsGroup group={group} index={index} />
              <Divider className="and-divider">
                <Chip label="AND" />
              </Divider>
            </Box>
          ))}
        <Box key="">
          <AddCriteriaButton kind={props.kind} />
        </Box>
      </Stack>
    </Grid>
  );
}

function AddCriteriaButton(props: {
  group?: string;
  kind?: tanagra.GroupKindEnum;
}) {
  const underlay = useUnderlay();
  const source = useSource();
  const cohort = useCohort();
  const history = useHistory();
  const dispatch = useAppDispatch();

  const configs = underlay.uiConfiguration.criteriaConfigs;

  const onAddCriteria = (criteria: tanagra.Criteria) => {
    let groupId = "";
    if (props.group) {
      groupId = props.group;
      dispatch(insertCriteria({ cohortId: cohort.id, groupId, criteria }));
    } else if (props.kind) {
      const action = dispatch(insertGroup(cohort.id, props.kind, criteria));
      groupId = action.payload.group.id;
    }
    if (groupId) {
      history.push(
        createUrl({
          underlayName: underlay.name,
          cohortId: cohort.id,
          groupId,
          criteriaId: criteria.id,
        })
      );
    }
  };

  const [menu, show] = useMenu({
    children: configs.map((config) => (
      <MenuItem
        key={config.title}
        onClick={() => {
          onAddCriteria(createCriteria(source, config));
        }}
      >
        {config.title}
      </MenuItem>
    )),
  });

  return (
    <>
      <Button onClick={show} variant="contained" className="add-criteria">
        Add Criteria
      </Button>
      {menu}
    </>
  );
}

function ParticipantsGroup(props: { group: tanagra.Group; index: number }) {
  const dispatch = useAppDispatch();
  const source = useSource();
  const underlay = useUnderlay();
  const cohort = useCohort();
  const groupName = props.group.name || "Group " + String(props.index + 1);
  const api = useContext(EntityCountsApiContext);

  const [renameGroupDialog, showRenameGroup] = useTextInputDialog({
    title: "Edit Group Name",
    initialText: groupName,
    textLabel: "Group Name",
    buttonLabel: "Rename Group",
    onConfirm: (name: string) => {
      dispatch(
        renameGroup({
          cohortId: cohort.id,
          groupId: props.group.id,
          groupName: name,
        })
      );
    },
  });

  const [groupMenu, groupShow] = useMenu({
    children: [
      <MenuItem key="1" onClick={showRenameGroup}>
        Edit Group Name
      </MenuItem>,
      <MenuItem
        key="2"
        onClick={() =>
          dispatch(
            deleteGroup({
              cohortId: cohort.id,
              groupId: props.group.id,
            })
          )
        }
      >
        Delete Group
      </MenuItem>,
    ],
  });

  const fetchGroupCount = useCallback(async () => {
    const groupForFilter: tanagra.Group = {
      id: props.group.id,
      kind: tanagra.GroupKindEnum.Included,
      criteria: props.group.criteria,
    };

    const cohortForFilter: tanagra.Cohort = {
      id: cohort.id,
      name: cohort.name,
      underlayName: cohort.underlayName,
      groups: [groupForFilter],
    };

    const searchEntityCountsRequest: tanagra.SearchEntityCountsRequest = {
      entityCounts: {
        entityVariable: "p",
        additionalSelectedAttributes: [],
        groupByAttributes: [],
        filter: generateQueryFilter(source, cohortForFilter, "p"),
      },
    };

    const data = await api.searchEntityCounts({
      underlayName: underlay.name,
      entityName: "person",
      searchEntityCountsRequest: searchEntityCountsRequest,
    });
    return data.counts?.[0].count;
  }, [underlay, cohort]);

  const groupCountState = useAsyncWithApi(fetchGroupCount);

  return (
    <Paper className="participants-group">
      <Grid container className="group-title">
        <Grid item xs="auto">
          <IconButton onClick={groupShow} component="span" size="small">
            <MoreVertIcon fontSize="small" />
          </IconButton>
          {groupMenu}
          {renameGroupDialog}
        </Grid>
        <Grid item>
          <Typography variant="h5">{groupName}</Typography>
        </Grid>
      </Grid>
      <Stack spacing={0}>
        {props.group.criteria.map((criteria) => (
          <Box key={criteria.id}>
            <ParticipantCriteria group={props.group} criteria={criteria} />
            <Divider>OR</Divider>
          </Box>
        ))}
        <Box
          key=""
          display={"flex"}
          flexDirection={"row"}
          justifyContent={"space-between"}
          alignItems="center"
        >
          <AddCriteriaButton group={props.group.id} />
          {
            <Loading status={groupCountState}>
              <Typography fontWeight={"bold"}>
                Group Count: {groupCountState.data?.toLocaleString()}
              </Typography>
            </Loading>
          }
        </Box>
      </Stack>
    </Paper>
  );
}

function ParticipantCriteria(props: {
  group: tanagra.Group;
  criteria: tanagra.Criteria;
}) {
  const source = useSource();
  const underlay = useUnderlay();
  const cohort = useCohort();
  const dispatch = useAppDispatch();
  const api = useContext(EntityCountsApiContext);

  const [renameDialog, showRenameCriteria] = useTextInputDialog({
    title: "Edit Criteria Name",
    initialText: props.criteria.name,
    textLabel: "Criteria Name",
    buttonLabel: "Confirm",
    onConfirm: (name: string) => {
      dispatch(
        renameCriteria({
          cohortId: cohort.id,
          groupId: props.group.id,
          criteriaId: props.criteria.id,
          criteriaName: name,
        })
      );
    },
  });

  const [menu, show] = useMenu({
    children: [
      <MenuItem
        key="1"
        onClick={() => {
          dispatch(
            deleteCriteria({
              cohortId: cohort.id,
              groupId: props.group.id,
              criteriaId: props.criteria.id,
            })
          );
        }}
      >
        Delete Criteria
      </MenuItem>,
      <MenuItem key="2" onClick={showRenameCriteria}>
        Edit Criteria Name
      </MenuItem>,
    ],
  });

  const fetchCriteriaCount = useCallback(async () => {
    const groupForFilter: tanagra.Group = {
      id: props.group.id,
      kind: tanagra.GroupKindEnum.Included,
      criteria: [props.criteria],
    };

    const cohortForFilter: tanagra.Cohort = {
      id: cohort.id,
      name: cohort.name,
      underlayName: cohort.underlayName,
      groups: [groupForFilter],
    };

    const searchEntityCountsRequest: tanagra.SearchEntityCountsRequest = {
      entityCounts: {
        entityVariable: "p",
        additionalSelectedAttributes: [],
        groupByAttributes: [],
        filter: generateQueryFilter(source, cohortForFilter, "p"),
      },
    };

    const data = await api.searchEntityCounts({
      underlayName: underlay.name,
      entityName: "person",
      searchEntityCountsRequest: searchEntityCountsRequest,
    });
    return data.counts?.[0].count;
  }, [underlay, cohort]);

  const criteriaCountState = useAsyncWithApi(fetchCriteriaCount);

  return (
    <Grid container>
      <Grid item xs="auto">
        <IconButton onClick={show} component="span" size="small">
          <MoreVertIcon fontSize="small" />
        </IconButton>
        {menu}
        {renameDialog}
      </Grid>
      <Grid item xs>
        <Accordion
          disableGutters={true}
          square={true}
          sx={{ boxShadow: 0 }}
          className="criteria-accordion"
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Link
              variant="h6"
              color="inherit"
              underline="hover"
              component={RouterLink}
              to={createUrl({
                underlayName: underlay.name,
                cohortId: cohort.id,
                groupId: props.group.id,
                criteriaId: props.criteria.id,
              })}
            >
              {props.criteria.name}
            </Link>
            <Divider orientation="vertical" variant="middle" flexItem />
            {
              <Loading status={criteriaCountState}>
                <Typography variant="body1">
                  {criteriaCountState.data?.toLocaleString()}
                </Typography>
              </Loading>
            }
          </AccordionSummary>
          <AccordionDetails>
            {getCriteriaPlugin(props.criteria).renderDetails()}
          </AccordionDetails>
        </Accordion>
      </Grid>
    </Grid>
  );
}

type DemographicChartsProps = {
  cohort: tanagra.Cohort;
};

function DemographicCharts({ cohort }: DemographicChartsProps) {
  const underlay = useUnderlay();
  const source = useSource();

  const api = useContext(EntityCountsApiContext);

  const barColours = [
    "#003f5c",
    "#2f4b7c",
    "#665191",
    "#a05195",
    "#d45087",
    "#f95d6a",
    "#ff7c43",
    "#ffa600",
  ];

  const fetchDemographicData = useCallback(async () => {
    const searchEntityCountsRequest: tanagra.SearchEntityCountsRequest = {
      entityCounts: {
        entityVariable: "p",
        additionalSelectedAttributes: ["gender", "race"],
        groupByAttributes: [
          "gender_concept_id",
          "race_concept_id",
          "year_of_birth",
        ],
        filter: generateQueryFilter(source, cohort, "p"),
      },
    };

    const data = await api.searchEntityCounts({
      underlayName: underlay.name,
      entityName: "person",
      searchEntityCountsRequest: searchEntityCountsRequest,
    });

    if (!data.counts) {
      throw new Error(
        "The counts property returned by the searchEntityCounts API is undefined."
      );
    }

    const demographicData = data.counts;
    let totalCount = 0;
    const demographicsByGender = new Map();
    const demographicsByGenderAgeRace = new Map();
    const demographicRaces = new Set();

    for (let i = 0; i < demographicData.length; i++) {
      const count = demographicData[i].count ?? 0;
      const gender = demographicData[i].definition?.gender.stringVal ?? "Other";
      const race = demographicData[i].definition?.race.stringVal ?? "Other";
      const currentYear = new Date().getFullYear();
      const yob = demographicData[i].definition?.year_of_birth?.int64Val ?? 0;
      const age = parseInt(currentYear.toString()) - yob;
      let ageRange = "";
      if (age >= 65) {
        ageRange = "65+";
      } else if (age >= 45 && age <= 64) {
        ageRange = "45-64";
      } else if (age <= 44) {
        ageRange = "18-44";
      }

      // Accumulate total count for results
      totalCount += count;

      // Configure hash map to represent data for gender chart
      if (!demographicsByGender.has(gender)) {
        demographicsByGender.set(gender, 0);
      }
      demographicsByGender.set(
        gender,
        demographicsByGender.get(gender) + count
      );

      // Configure hash map to represent data for gender / age / race chart
      const genderAgeRange = `${gender} ${ageRange}`;

      if (!demographicsByGenderAgeRace.has(genderAgeRange)) {
        demographicsByGenderAgeRace.set(genderAgeRange, new Map());
      }

      if (!demographicsByGenderAgeRace.get(genderAgeRange).has(race)) {
        demographicsByGenderAgeRace.get(genderAgeRange).set(race, 0);
      }

      const currCountForRace = demographicsByGenderAgeRace
        .get(genderAgeRange)
        .get(race);

      demographicsByGenderAgeRace
        .get(genderAgeRange)
        .set(race, currCountForRace + count);

      // Configure set of races returned in results
      if (!demographicRaces.has(race)) {
        demographicRaces.add(race);
      }
    }

    return {
      totalCount,
      demographicsByGender: Array.from(demographicsByGender, ([key, value]) => {
        return {
          name: key,
          count: value,
          color: barColours[0],
        };
      }),
      demographicRaces: Array.from(demographicRaces),
      demographicsByGenderAgeRace: Array.from(
        demographicsByGenderAgeRace,
        ([key, value]) => {
          return {
            ageRange: key,
            ...Object.fromEntries(value),
          };
        }
      ),
    };
  }, [underlay, cohort]);

  const demographicState = useAsyncWithApi(fetchDemographicData);

  const tickFormatter = (value: string) => {
    return value.length > 15 ? value.substr(0, 15).concat("…") : value;
  };

  return (
    <>
      <Loading status={demographicState}>
        <Grid item xs={1}>
          <Stack>
            <Typography variant="h4">{`Total Count: ${demographicState.data?.totalCount.toLocaleString()}`}</Typography>
            <Typography>Gender Identity</Typography>
            <ResponsiveContainer width="90%" height={250}>
              <BarChart
                data={demographicState.data?.demographicsByGender}
                margin={{
                  top: 10,
                  right: 0,
                  left: 20,
                  bottom: 10,
                }}
                layout="vertical"
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" />
                <YAxis
                  dataKey="name"
                  type="category"
                  width={150}
                  tickFormatter={tickFormatter}
                  tickMargin={10}
                />
                <Tooltip />
                <Bar dataKey={"count"} fill="#2f4b7c" maxBarSize={60} />
              </BarChart>
            </ResponsiveContainer>
            <Typography>Gender Identity, Current Age, Race</Typography>
            <ResponsiveContainer width="100%" height={400}>
              <BarChart
                data={demographicState.data?.demographicsByGenderAgeRace}
                margin={{
                  top: 10,
                  right: 0,
                  left: 20,
                  bottom: 10,
                }}
                layout="vertical"
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" />
                <YAxis
                  dataKey="ageRange"
                  type="category"
                  width={150}
                  tickFormatter={tickFormatter}
                  tickMargin={10}
                />
                <Tooltip />
                {demographicState.data?.demographicRaces.map((race, index) => (
                  <Bar
                    key={index}
                    dataKey={race as string}
                    stackId="a"
                    fill={barColours[index % barColours.length]}
                    maxBarSize={100}
                  />
                ))}
              </BarChart>
            </ResponsiveContainer>
          </Stack>
        </Grid>
      </Loading>
    </>
  );
}
