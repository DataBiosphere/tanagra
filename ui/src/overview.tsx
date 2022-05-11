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
import {
  deleteCriteria,
  deleteGroup,
  insertCriteria,
  insertGroup,
  renameCriteria,
  renameGroup,
} from "cohortsSlice";
import { useMenu } from "components/menu";
import { useTextInputDialog } from "components/textInputDialog";
import { useAppDispatch, useCohort, useUnderlay } from "hooks";
import React from "react";
import { Link as RouterLink, useHistory } from "react-router-dom";
import { createUrl } from "router";
import {
  createCriteria,
  Criteria,
  getCriteriaPlugin,
  Group,
  GroupKind,
} from "./cohort";

export default function Overview() {
  const cohort = useCohort();

  return (
    <>
      <ActionBar title={cohort.name} />
      <Grid container columns={3} className="overview">
        <Grid item xs={1}>
          <Typography variant="h4">Included Participants</Typography>
          <Stack spacing={0}>
            {cohort.groups
              .filter((g) => g.kind === GroupKind.Included)
              .map((group, index) => (
                <Box key={group.id}>
                  <ParticipantsGroup group={group} index={index} />
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
  const underlay = useUnderlay();
  const cohort = useCohort();
  const history = useHistory();
  const dispatch = useAppDispatch();

  const configs = underlay.criteriaConfigs;

  const onAddCriteria = (criteria: Criteria) => {
    let groupId = "";
    if (typeof props.group === "string") {
      groupId = props.group;
      dispatch(insertCriteria({ cohortId: cohort.id, groupId, criteria }));
    } else {
      const action = dispatch(insertGroup(cohort.id, props.group, criteria));
      groupId = action.payload.group.id;
    }
    history.push(
      createUrl({
        underlayName: underlay.name,
        cohortId: cohort.id,
        groupId,
        criteriaId: criteria.id,
      })
    );
  };

  const [menu, show] = useMenu({
    children: configs.map((config) => (
      <MenuItem
        key={config.title}
        onClick={() => {
          onAddCriteria(createCriteria(underlay, config));
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

function ParticipantsGroup(props: { group: Group; index: number }) {
  const dispatch = useAppDispatch();
  const cohort = useCohort();
  const groupName = props.group.name || "Group " + String(props.index + 1);
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
        <Box key="">
          <AddCriteriaButton group={props.group.id} />
        </Box>
      </Stack>
    </Paper>
  );
}

function ParticipantCriteria(props: { group: Group; criteria: Criteria }) {
  const underlay = useUnderlay();
  const cohort = useCohort();
  const dispatch = useAppDispatch();

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
