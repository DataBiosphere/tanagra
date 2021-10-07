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
import {
  bindMenu,
  bindTrigger,
  usePopupState,
} from "material-ui-popup-state/hooks";
import React from "react";
import { Link as RouterLink } from "react-router-dom";
import { Criteria, Dataset, Group, GroupKind } from "./dataset";

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
              <AddCriteriaButton id="" />
            </Box>
          </Stack>
        </Grid>
      </Grid>
    </>
  );
}

function AddCriteriaButton(props: { id: string }) {
  const addCriteriaState = usePopupState({
    variant: "popover",
    popupId: "addCriteria",
  });

  const onAddCriteria = () => {
    console.log(
      "Group: " + addCriteriaState.anchorEl?.getAttribute("data-group")
    );
    addCriteriaState.close();
  };

  return (
    <>
      <Button
        data-group={props.id}
        variant="contained"
        {...bindTrigger(addCriteriaState)}
      >
        Add Criteria
      </Button>
      <Menu {...bindMenu(addCriteriaState)}>
        <MenuItem onClick={onAddCriteria}>Pie</MenuItem>
        <MenuItem onClick={onAddCriteria}>Death</MenuItem>
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
          <AddCriteriaButton id={props.group.id} />
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
