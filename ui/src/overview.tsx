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
import { useAppDispatch } from "hooks";
import React from "react";
import { Link as RouterLink, useHistory, useParams } from "react-router-dom";
import * as tanagra from "tanagra-api";
import {
  Cohort,
  createCriteria,
  Criteria,
  getCriteriaPlugin,
  Group,
  GroupKind,
} from "./cohort";

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

  const onAddCriteria = (criteria: Criteria) => {
    let groupId = "";
    if (typeof props.group === "string") {
      groupId = props.group;
      dispatch(insertCriteria({ cohortId, groupId, criteria }));
    } else {
      const action = dispatch(insertGroup(cohortId, props.group, criteria));
      groupId = action.payload.group.id;
    }
    history.push(editRoute(cohortId, groupId, criteria.id));
  };
  // TODO(tjennison): Fetch configs from the backend.
  const columns = [
    { key: "concept_name", width: "100%", title: "Concept Name" },
    { key: "concept_id", width: 120, title: "Concept ID" },
    { key: "standard_concept", width: 180, title: "Source/Standard" },
    { key: "vocabulary_id", width: 120, title: "Vocab" },
    { key: "concept_code", width: 120, title: "Code" },
  ];

  const configs = [
    {
      type: "concept",
      title: "Conditions",
      defaultName: "Contains Conditions Codes",
      plugin: {
        columns,
        entities: [{ name: "condition", selectable: true, hierarchical: true }],
      },
    },
    {
      type: "concept",
      title: "Procedures",
      defaultName: "Contains Procedures Codes",
      plugin: {
        columns,
        entities: [{ name: "procedure", selectable: true, hierarchical: true }],
      },
    },
    {
      type: "concept",
      title: "Observations",
      defaultName: "Contains Observations Codes",
      plugin: {
        columns,
        entities: [{ name: "observation", selectable: true }],
      },
    },
    {
      type: "concept",
      title: "Drugs",
      defaultName: "Contains Drugs Codes",
      plugin: {
        columns,
        entities: [
          { name: "ingredient", selectable: true, hierarchical: true },
          {
            name: "brand",
            sourceConcepts: true,
            attributes: [
              "concept_name",
              "concept_id",
              "standard_concept",
              "concept_code",
            ],
            listChildren: {
              entity: "ingredient",
              idPath: "relationshipFilter.filter.binaryFilter.attributeValue",
              filter: {
                relationshipFilter: {
                  outerVariable: "ingredient",
                  newVariable: "brand",
                  newEntity: "brand",
                  filter: {
                    binaryFilter: {
                      attributeVariable: {
                        variable: "brand",
                        name: "concept_id",
                      },
                      operator: tanagra.BinaryFilterOperator.Equals,
                      attributeValue: {
                        int64Val: 0,
                      },
                    },
                  },
                },
              },
            },
          },
        ],
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
