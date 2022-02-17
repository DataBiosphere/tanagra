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
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { deleteCriteria, insertCriteria, insertGroup } from "cohortsSlice";
import { useAppDispatch } from "hooks";
import React, { useCallback } from "react";
import { Link as RouterLink, useHistory, useParams } from "react-router-dom";
import ActionBar from "./actionBar";
import {
  Cohort,
  createCriteria,
  Criteria,
  getCriteriaPlugin,
  Group,
  GroupKind,
} from "./cohort";
import { useMenu } from "./menu";

function editRoute(
  cohortId: string,
  groupId: string,
  criteriaId: string
): string {
  return `/cohort/${cohortId}/edit/${groupId}/${criteriaId}`;
}

type OverviewProps = {
  cohort: Cohort;
};

export default function Overview(props: OverviewProps) {
  return (
    <>
      <ActionBar backUrl="/" title={props.cohort.name} cohort={props.cohort} />
      <Grid container columns={3} className="overview">
        <Grid item xs={1}>
          <Typography variant="h4">Included Participants</Typography>
          <Stack spacing={0}>
            {props.cohort.groups
              .filter((g) => g.kind === GroupKind.Included)
              .map((group) => (
                <Box key={group.id}>
                  <ParticipantsGroup group={group} />
                  <Divider className="and-divider">
                    <Chip label="AND" />
                  </Divider>
                </Box>
              ))}
            <Box key="">
              <AddCriteriaButton group={GroupKind.Included} />
            </Box>
          </Stack>
        </Grid>
      </Grid>
    </>
  );
}

// If group is a string, the criteria is added to the group with that id. If
// it's a GroupKind, a new group of that kind is added instead.
function AddCriteriaButton(props: { group: string | GroupKind }) {
  const { cohortId } = useParams<{ cohortId: string }>();
  const history = useHistory();
  const dispatch = useAppDispatch();

  const onAddCriteria = useCallback(
    (criteria: Criteria) => {
      let groupId = "";
      if (typeof props.group === "string") {
        groupId = props.group;
        dispatch(insertCriteria({ cohortId, groupId, criteria }));
      } else {
        const action = dispatch(insertGroup(cohortId, props.group, criteria));
        groupId = action.payload.group.id;
      }
      history.push(editRoute(cohortId, groupId, criteria.id));
    },
    [props.group]
  );

  // TODO(tjennison): Fetch configs from the backend.
  const configs = [
    {
      type: "concept",
      title: "Conditions",
      defaultName: "Contains Conditions Codes",
      plugin: {
        entity: "condition",
        hierarchical: true,
      },
    },
    {
      type: "concept",
      title: "Procedures",
      defaultName: "Contains Procedures Codes",
      plugin: {
        entity: "procedure",
        hierarchical: true,
      },
    },
    {
      type: "concept",
      title: "Observations",
      defaultName: "Contains Observtions Codes",
      plugin: {
        entity: "observation",
      },
    },
  ];

  const [menu, show] = useMenu({
    children: configs.map((config) => (
      <MenuItem
        key={config.title}
        onClick={() => {
          onAddCriteria(createCriteria(config));
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

function ParticipantsGroup(props: { group: Group }) {
  return (
    <Paper className="participants-group">
      <Stack spacing={0}>
        {props.group.criteria.map((criteria) => (
          <Box key={criteria.id}>
            <ParticipantCriteria group={props.group} criteria={criteria} />
            <Divider>OR</Divider>
          </Box>
        ))}
        <Box key="">
          <AddCriteriaButton group={props.group.id} />
        </Box>
      </Stack>
    </Paper>
  );
}

function ParticipantCriteria(props: { group: Group; criteria: Criteria }) {
  const { cohortId } = useParams<{ cohortId: string }>();
  const dispatch = useAppDispatch();

  const [menu, show] = useMenu({
    children: [
      <MenuItem
        key="1"
        component={RouterLink}
        to={editRoute(cohortId, props.group.id, props.criteria.id)}
      >
        Edit Criteria
      </MenuItem>,
      <MenuItem
        key="2"
        onClick={() => {
          dispatch(
            deleteCriteria({
              cohortId,
              groupId: props.group.id,
              criteriaId: props.criteria.id,
            })
          );
        }}
      >
        Delete Criteria
      </MenuItem>,
    ],
  });

  return (
    <Grid container>
      <Grid item xs="auto">
        <IconButton onClick={show} component="span" size="small">
          <MoreVertIcon fontSize="small" />
        </IconButton>
        {menu}
      </Grid>
      <Grid item xs>
        <Accordion
          disableGutters={true}
          square={true}
          sx={{ boxShadow: 0 }}
          className="criteria-accordion"
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">{props.criteria.name}</Typography>
            <Divider orientation="vertical" variant="middle" flexItem />
            <Typography variant="body1">{props.criteria.count}</Typography>
          </AccordionSummary>
          <AccordionDetails>
            {getCriteriaPlugin(props.criteria).renderDetails()}
          </AccordionDetails>
        </Accordion>
      </Grid>
    </Grid>
  );
}
