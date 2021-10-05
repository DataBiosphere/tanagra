import Button from "@material-ui/core/Button";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import {
  bindMenu,
  bindTrigger,
  usePopupState,
} from "material-ui-popup-state/hooks";
import React from "react";
import { Criteria, DataSet, Group } from "./dataSet";

type OverviewProps = {
  underlayName: string;
  dataSet: DataSet;
};

export default function Overview(props: OverviewProps) {
  return (
    <>
      <Grid container columns={3}>
        <Grid item xs={1} sx={{ mx: 2 }}>
          <Typography variant="h4">Included Participants</Typography>
          <Stack spacing={0}>
            {props.dataSet.included.map((group) => (
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
      <Typography variant="h5">Group {props.group.id.slice(0, 8)}</Typography>
      <Stack spacing={0}>
        {props.group.criteria.map((criteria) => (
          <Box key={criteria.id}>
            <ParticipantCriteria criteria={criteria} />
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

function ParticipantCriteria(props: { criteria: Criteria }) {
  return (
    <Typography variant="h6">
      {props.criteria.code}: {props.criteria.name}
    </Typography>
  );
}
