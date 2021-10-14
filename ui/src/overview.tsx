import Button from "@material-ui/core/Button";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import Accordion from "@mui/material/Accordion";
import AccordionDetails from "@mui/material/AccordionDetails";
import AccordionSummary from "@mui/material/AccordionSummary";
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { ConceptCriteria } from "criteria/concept";
import {
  bindMenu,
  bindTrigger,
  usePopupState,
} from "material-ui-popup-state/hooks";
import React, { useCallback } from "react";
import { Link as RouterLink } from "react-router-dom";
import { Criteria, Dataset, Group, GroupKind } from "./dataset";
import { useDatasetUpdater } from "./datasetUpdaterContext";

type OverviewProps = {
  dataset: Dataset;
};

export default function Overview(props: OverviewProps) {
  return (
    <>
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

function AddCriteriaButton(props: { group: string | GroupKind }) {
  const addCriteriaState = usePopupState({
    variant: "popover",
    popupId: "addCriteria",
  });

  const updater = useDatasetUpdater();

  const onAddCriteria = useCallback(
    (create: () => Criteria) => {
      addCriteriaState.close();

      updater.update((dataset: Dataset) => {
        if (typeof props.group === "string") {
          dataset.addCriteria(props.group, create());
        } else {
          dataset.addGroupAndCriteria(props.group, create());
        }
      });
    },
    [updater, addCriteriaState]
  );

  const items = [
    {
      title: "Conditions",
      create: () =>
        new ConceptCriteria("Contains Condition Code", "condition_occurrence"),
    },
  ];

  return (
    <>
      <Button variant="contained" {...bindTrigger(addCriteriaState)}>
        Add Criteria
      </Button>
      <Menu {...bindMenu(addCriteriaState)}>
        {items.map((item) => {
          return (
            <MenuItem
              key={item.title}
              onClick={() => {
                onAddCriteria(item.create);
              }}
            >
              {item.title}
            </MenuItem>
          );
        })}
      </Menu>
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
  const popupState = usePopupState({
    variant: "popover",
    popupId: "criteria",
  });

  return (
    <Stack direction="row" alignItems="flex-start">
      <IconButton {...bindTrigger(popupState)}>
        <MoreVertIcon />
      </IconButton>
      <Menu {...bindMenu(popupState)}>
        <MenuItem
          component={RouterLink}
          to={`/edit/${props.group.id}/${props.criteria.id}`}
        >
          Edit Criteria
        </MenuItem>
      </Menu>
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
