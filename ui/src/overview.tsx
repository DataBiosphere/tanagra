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
import ActionBar from "actionBar";
import { deleteCriteria, insertCriteria, insertGroup } from "cohortsSlice";
import { useMenu } from "components/menu";
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
  Item,
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
  const underlay = useUnderlay();
  const cohort = useCohort();
  const history = useHistory();
  const dispatch = useAppDispatch();

  const configs = underlay.criteriaMenu;

  const [, setAnchorEl] = React.useState<HTMLElement | undefined>();

  const onAddCriteria = (criteria: Criteria) => {
    let groupId: string;
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

  const handleClick = (
    event: React.MouseEvent<HTMLElement | undefined, MouseEvent>,
    config: Item
  ) => {
    if (
      typeof config.criteriaConfig !== "undefined" &&
      typeof config.subItems === "undefined"
    ) {
      onAddCriteria(createCriteria(config.criteriaConfig));
      setAnchorEl(event.currentTarget);
      // return show(event);
    } else {
      return showSub(event);
    }
  };

  const [menu, show] = useMenu(
    {
      children: configs.map((config) => (
        <MenuItem
          key={config?.title}
          onClick={(event) => {
            handleClick(event, config);
          }}
        >
          {config.title || config.criteriaConfig?.name}
        </MenuItem>
      )),
    },
    false
  );

  const subMenuConfig =
    configs[
      configs.findIndex((item) => {
        return typeof item.subItems !== "undefined";
      })
    ];
  const [subMenu, showSub] = useMenu({
    anchorOrigin: {
      vertical: "top",
      horizontal: "right",
    },
    children: subMenuConfig.subItems?.map((config) => (
      <MenuItem
        key={config.criteriaConfig?.name}
        onClick={(event) => {
          if (typeof config.criteriaConfig !== "undefined") {
            onAddCriteria(createCriteria(config.criteriaConfig));
          }
        }}
      >
        {config?.criteriaConfig?.name}
      </MenuItem>
    )),
  });

  return (
    <>
      <Button onClick={show} variant="contained" className="add-criteria">
        Add Criteria
      </Button>
      {menu}
      {subMenu}
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
  const underlay = useUnderlay();
  const cohort = useCohort();
  const dispatch = useAppDispatch();

  const [menu, show] = useMenu({
    children: [
      <MenuItem
        key="1"
        component={RouterLink}
        to={createUrl({
          underlayName: underlay.name,
          cohortId: cohort.id,
          groupId: props.group.id,
          criteriaId: props.criteria.id,
        })}
      >
        Edit Criteria
      </MenuItem>,
      <MenuItem
        key="2"
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
