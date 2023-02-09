import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  deleteCohortCriteria,
  deleteCohortGroup,
  insertCohortGroup,
  updateCohortGroup,
  useCohortContext,
} from "cohortContext";
import { defaultFilter } from "cohortsSlice";
import CohortToolbar from "cohortToolbar";
import Empty from "components/empty";
import Loading from "components/loading";
import { useTextInputDialog } from "components/textInputDialog";
import { useSource } from "data/source";
import { DemographicCharts } from "demographicCharts";
import { useCohort } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { cohortURL, criteriaURL, exitURL, useBaseParams } from "router";
import useSWRImmutable from "swr/immutable";
import * as tanagra from "tanagra-api";
import {
  generateCohortFilter,
  getCriteriaPlugin,
  getCriteriaTitle,
  groupName,
} from "./cohort";

export function Overview() {
  return (
    <GridLayout cols="auto 380px">
      <GridBox sx={{ p: 1 }}>
        <GroupList />
      </GridBox>
      <GridBox
        sx={{
          p: 1,
          backgroundColor: (theme) => theme.palette.background.paper,
          leftBorderStyle: "solid",
          borderColor: (theme) => theme.palette.divider,
          borderWidth: "1px",
        }}
      >
        <DemographicCharts />
      </GridBox>
    </GridLayout>
  );
}

function GroupDivider() {
  return (
    <Divider variant="middle">
      <Chip label="AND" />
    </Divider>
  );
}

function GroupList() {
  const context = useCohortContext();
  const cohort = useCohort();
  const params = useBaseParams();

  return (
    <Box className="outline">
      <ActionBar
        title={cohort.name}
        extraControls={<CohortToolbar />}
        backURL={exitURL(params)}
      />
      <Typography variant="h2">
        To be included in the cohort, participants…
      </Typography>
      <List sx={{ p: 0 }}>
        {cohort.groups.map((g, index) => (
          <>
            {index !== 0 ? <GroupDivider /> : null}
            <ListItem disableGutters>
              <ParticipantsGroup group={g} groupIndex={index} />
            </ListItem>
          </>
        ))}
        {cohort.groups.length > 1 || cohort.groups[0].criteria.length > 0 ? (
          <>
            <GroupDivider />
            <ListItem disableGutters key="" sx={{ p: 0 }}>
              <Button
                onClick={() => insertCohortGroup(context)}
                variant="contained"
              >
                Add group
              </Button>
            </ListItem>
          </>
        ) : null}
      </List>
    </Box>
  );
}

function ParticipantsGroup(props: {
  group: tanagra.Group;
  groupIndex: number;
}) {
  const source = useSource();
  const cohort = useCohort();
  const context = useCohortContext();
  const navigate = useNavigate();

  const fetchGroupCount = useCallback(async () => {
    const cohortForFilter: tanagra.Cohort = {
      id: cohort.id,
      name: cohort.name,
      underlayName: cohort.underlayName,
      groups: [props.group],
    };

    const filter = generateCohortFilter(cohortForFilter);
    if (!filter) {
      return 0;
    }

    return (await source.filterCount(filter))[0].count;
  }, [cohort.id, cohort.name, cohort.underlayName, props.group]);

  const groupCountState = useSWRImmutable(
    {
      component: "Overview",
      cohortId: cohort.id,
      cohortName: cohort.name,
      underlayName: cohort.underlayName,
      group: props.group,
    },
    fetchGroupCount
  );
  const name = groupName(props.group, props.groupIndex);

  const [renameGroupDialog, showRenameGroup] = useTextInputDialog({
    title: "Edit Group Name",
    initialText: name,
    textLabel: "Group name",
    buttonLabel: "Rename group",
    onConfirm: (name: string) => {
      updateCohortGroup(context, props.group.id, name);
    },
  });

  return (
    <Paper sx={{ width: "100%", overflow: "hidden" }}>
      <Stack>
        <Stack
          direction="row"
          alignItems="center"
          justifyContent="space-between"
          sx={{ p: 1, backgroundColor: (theme) => theme.palette.divider }}
        >
          <Stack direction="row" alignItems="baseline">
            <FormControl>
              <Select
                value={props.group.filter.excluded ? 1 : 0}
                onChange={(event: SelectChangeEvent<number>) => {
                  updateCohortGroup(context, props.group.id, undefined, {
                    ...props.group.filter,
                    excluded: event.target.value === 1,
                  });
                }}
              >
                <MenuItem value={0}>Must</MenuItem>
                <MenuItem value={1}>Must not</MenuItem>
              </Select>
            </FormControl>
            <Typography variant="body1">&nbsp;meet&nbsp;</Typography>
            <FormControl>
              <Select
                value={props.group.filter.kind}
                onChange={(event: SelectChangeEvent<string>) => {
                  updateCohortGroup(context, props.group.id, undefined, {
                    ...props.group.filter,
                    kind: event.target.value as tanagra.GroupFilterKindEnum,
                  });
                }}
              >
                <MenuItem value={tanagra.GroupFilterKindEnum.Any}>any</MenuItem>
                <MenuItem value={tanagra.GroupFilterKindEnum.All}>all</MenuItem>
              </Select>
            </FormControl>
            <Typography variant="body1">
              &nbsp;of the following criteria:
            </Typography>
          </Stack>
          <Stack direction="row" alignItems="center">
            <Typography variant="h3" sx={{ mr: 1 }}>
              {name}
            </Typography>
            <IconButton onClick={showRenameGroup}>
              <EditIcon />
            </IconButton>
            {renameGroupDialog}
            <IconButton
              onClick={() => {
                deleteCohortGroup(context, props.group.id);
              }}
            >
              <DeleteIcon />
            </IconButton>
          </Stack>
        </Stack>
        <Stack sx={{ p: 1 }}>
          {props.group.criteria.length === 0 ? (
            <Empty
              maxWidth="90%"
              minHeight="60px"
              title="No criteria yet"
              subtitle="You can add a criteria by clicking on 'Add criteria'"
            />
          ) : (
            props.group.criteria.map((criteria) => (
              <>
                <Box key={criteria.id}>
                  <ParticipantCriteria
                    group={props.group}
                    criteria={criteria}
                  />
                </Box>
                <Divider sx={{ my: 1 }} />
              </>
            ))
          )}
          <Stack direction="row">
            <Button
              onClick={() =>
                navigate(`../${cohortURL(cohort.id, props.group.id)}/add`)
              }
              variant="contained"
            >
              Add criteria
            </Button>
          </Stack>
        </Stack>
        <Stack
          direction="row"
          justifyContent="right"
          alignItems="center"
          sx={{ p: 1, backgroundColor: (theme) => theme.palette.divider }}
        >
          <Typography variant="subtitle1">Group count:&nbsp;</Typography>
          <Loading status={groupCountState} size="small">
            {groupCountState.data?.toLocaleString()}
          </Loading>
        </Stack>
      </Stack>
    </Paper>
  );
}

function ParticipantCriteria(props: {
  group: tanagra.Group;
  criteria: tanagra.Criteria;
}) {
  const source = useSource();
  const cohort = useCohort();
  const context = useCohortContext();

  const fetchCriteriaCount = useCallback(async () => {
    const cohortForFilter: tanagra.Cohort = {
      id: cohort.id,
      name: cohort.name,
      underlayName: cohort.underlayName,
      groups: [
        {
          id: props.group.id,
          filter: defaultFilter,
          criteria: [props.criteria],
        },
      ],
    };

    const filter = generateCohortFilter(cohortForFilter);
    if (!filter) {
      return 0;
    }

    return (await source.filterCount(filter))[0].count;
  }, [cohort.id, cohort.name, cohort.underlayName, props.criteria]);

  const criteriaCountState = useSWRImmutable(
    {
      component: "Overview",
      cohortId: cohort.id,
      cohortName: cohort.name,
      underlayName: cohort.underlayName,
      criteria: props.criteria,
    },
    fetchCriteriaCount
  );

  const plugin = getCriteriaPlugin(props.criteria);
  const title = getCriteriaTitle(props.criteria, plugin);

  return (
    <Stack key={props.criteria.id}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Stack direction="row" alignItems="flex-start">
          {!!plugin.renderEdit ? (
            <Link
              variant="h4"
              color="inherit"
              underline="hover"
              component={RouterLink}
              to={criteriaURL(props.criteria.id)}
            >
              {title}
            </Link>
          ) : (
            <Typography variant="h4">{title}</Typography>
          )}
          <IconButton
            onClick={() => {
              deleteCohortCriteria(context, props.group.id, props.criteria.id);
            }}
          >
            <DeleteIcon fontSize="small" sx={{ mt: "-3px" }} />
          </IconButton>
        </Stack>
        <Loading status={criteriaCountState} size="small">
          <Typography variant="body2" sx={{ ml: 1 }}>
            {criteriaCountState.data?.toLocaleString()}
          </Typography>
        </Loading>
      </Stack>
      {plugin.renderInline(props.criteria.id)}
    </Stack>
  );
}
