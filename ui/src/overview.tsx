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
import { ConceptCriteria } from "criteria/concept";
import React, { useCallback } from "react";
import { Link as RouterLink, useHistory } from "react-router-dom";
import ActionBar from "./actionBar";
import { Cohort, Criteria, Group, GroupKind } from "./cohort";
import { useCohortUpdater } from "./cohortUpdaterContext";
import { useMenu } from "./menu";

function editRoute(groupId: string, criteriaId: string): string {
  return `/edit/${groupId}/${criteriaId}`;
}

type OverviewProps = {
  cohort: Cohort;
};

export default function Overview(props: OverviewProps) {
  return (
    <>
      <ActionBar title="Cohort" cohort={props.cohort} />
      <Grid container columns={3} className="overview">
        <Grid item xs={1}>
          <Typography variant="h4">Included Participants</Typography>
          <Stack spacing={0}>
            {props.cohort.listGroups(GroupKind.Included).map((group) => (
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
  const history = useHistory();
  const updater = useCohortUpdater();

  const onAddCriteria = useCallback(
    (create: () => Criteria) => {
      let groupId = "";
      const criteria = create();
      updater.update((cohort: Cohort) => {
        if (typeof props.group === "string") {
          cohort.addCriteria(props.group, criteria);
          groupId = props.group;
        } else {
          groupId = cohort.addGroupAndCriteria(props.group, criteria);
        }
      });
      history.push(editRoute(groupId, criteria.id));
    },
    [updater]
  );

  const items = [
    {
      title: "Conditions",
      create: () =>
        new ConceptCriteria("Contains Condition Code", "condition_occurrence"),
    },
  ];

  const [menu, show] = useMenu({
    children: items.map((item) => (
      <MenuItem
        key={item.title}
        onClick={() => {
          onAddCriteria(item.create);
        }}
      >
        {item.title}
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
  const updater = useCohortUpdater();

  const [menu, show] = useMenu({
    children: [
      <MenuItem
        key="1"
        component={RouterLink}
        to={editRoute(props.group.id, props.criteria.id)}
      >
        Edit Criteria
      </MenuItem>,
      <MenuItem
        key="2"
        onClick={() => {
          updater.update((cohort: Cohort) => {
            cohort.deleteCriteria(props.group.id, props.criteria.id);
          });
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
          <AccordionDetails>{props.criteria.renderDetails()}</AccordionDetails>
        </Accordion>
      </Grid>
    </Grid>
  );
}
