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
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { ConceptCriteria } from "criteria/concept";
import React, { useCallback } from "react";
import { Link as RouterLink, useHistory } from "react-router-dom";
import ActionBar from "./actionBar";
import { Criteria, Dataset, Group, GroupKind } from "./dataset";
import { useDatasetUpdater } from "./datasetUpdaterContext";
import { useMenu } from "./menu";

function editRoute(groupId: string, criteriaId: string): string {
  return `/edit/${groupId}/${criteriaId}`;
}

type OverviewProps = {
  dataset: Dataset;
};

export default function Overview(props: OverviewProps) {
  return (
    <>
      <ActionBar title="Cohort" />
      <Grid container columns={3}>
        <Grid item xs={1} sx={{ mx: 2 }}>
          <Typography variant="h4">Included Participants</Typography>
          <Stack spacing={0}>
            {props.dataset.listGroups(GroupKind.Included).map((group) => (
              <Box key={group.id}>
                <ParticipantsGroup group={group} />
                <Divider>
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
  const updater = useDatasetUpdater();

  const onAddCriteria = useCallback(
    (create: () => Criteria) => {
      let groupId = "";
      const criteria = create();
      updater.update((dataset: Dataset) => {
        if (typeof props.group === "string") {
          dataset.addCriteria(props.group, criteria);
          groupId = props.group;
        } else {
          groupId = dataset.addGroupAndCriteria(props.group, criteria);
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
    items: items.map((item) => {
      return {
        text: item.title,
        onClick: () => {
          onAddCriteria(item.create);
        },
      };
    }),
  });

  return (
    <>
      <Button onClick={show} variant="contained">
        Add Criteria
      </Button>
      {menu}
    </>
  );
}

function ParticipantsGroup(props: { group: Group }) {
  return (
    <Paper>
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
  const updater = useDatasetUpdater();

  const [menu, show] = useMenu({
    items: [
      {
        text: "Edit Criteria",
        props: {
          component: RouterLink,
          to: editRoute(props.group.id, props.criteria.id),
        },
      },
      {
        text: "Delete Criteria",
        onClick: () => {
          updater.update((dataset: Dataset) => {
            dataset.deleteCriteria(props.group.id, props.criteria.id);
          });
        },
      },
    ],
  });

  return (
    <Stack direction="row" alignItems="flex-start">
      <IconButton onClick={show} component="span" size="small">
        <MoreVertIcon fontSize="small" />
      </IconButton>
      {menu}
      <Accordion disableGutters={true} square={true}>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">
            {props.criteria.name}: {props.criteria.count}
          </Typography>
        </AccordionSummary>
        <AccordionDetails>{props.criteria.renderDetails()}</AccordionDetails>
      </Accordion>
    </Stack>
  );
}
