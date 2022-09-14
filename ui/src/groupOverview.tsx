import CheckIcon from "@mui/icons-material/Check";
import ClearIcon from "@mui/icons-material/Clear";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  deleteGroup,
  insertCriteria,
  renameGroup,
  setGroupKind,
} from "cohortsSlice";
import { useMenu } from "components/menu";
import { useTextInputDialog } from "components/textInputDialog";
import { useSource } from "data/source";
import {
  useAppDispatch,
  useCohort,
  useCohortAndGroup,
  useUnderlay,
} from "hooks";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { cohortURL, criteriaURL } from "router";
import * as tanagra from "tanagra-api";
import { createCriteria, getCriteriaPlugin, groupName } from "./cohort";

export function GroupOverview() {
  const { cohort, group, groupIndex } = useCohortAndGroup();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const name = groupName(group, groupIndex);

  const [renameGroupDialog, showRenameGroup] = useTextInputDialog({
    title: "Edit Group Name",
    initialText: name,
    textLabel: "Group Name",
    buttonLabel: "Rename Group",
    onConfirm: (name: string) => {
      dispatch(
        renameGroup({
          cohortId: cohort.id,
          groupId: group.id,
          groupName: name,
        })
      );
    },
  });

  return (
    <Box sx={{ m: 1 }}>
      <ActionBar title={cohort.name} />
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="baseline"
      >
        <Stack direction="row" alignItems="center">
          <Typography variant="h5">{name}</Typography>
          <IconButton onClick={showRenameGroup}>
            <EditIcon />
          </IconButton>
          {renameGroupDialog}
        </Stack>
        <Stack direction="row" alignItems="center">
          <ClearIcon />
          <Typography>Excluded</Typography>
          <Switch
            checked={group.kind === tanagra.GroupKindEnum.Included}
            onChange={(event, checked) =>
              dispatch(
                setGroupKind(
                  cohort.id,
                  group.id,
                  checked
                    ? tanagra.GroupKindEnum.Included
                    : tanagra.GroupKindEnum.Excluded
                )
              )
            }
            inputProps={{ "aria-label": "controlled" }}
          />
          <CheckIcon />
          <Typography>Included</Typography>
        </Stack>
      </Stack>
      <Divider />
      <Stack spacing={0}>
        {group.criteria.map((criteria) => {
          const plugin = getCriteriaPlugin(criteria);
          const title = `${criteria.config.title}: ${
            plugin.displayDetails().title
          }`;

          return (
            <Box key={criteria.id}>
              <Box sx={{ m: 1 }}>
                <Stack direction="row">
                  {!!plugin.renderEdit ? (
                    <Link
                      variant="h6"
                      color="inherit"
                      underline="hover"
                      component={RouterLink}
                      to={criteriaURL(criteria.id)}
                    >
                      {title}
                    </Link>
                  ) : (
                    <Typography variant="h6">{title}</Typography>
                  )}
                </Stack>
                {plugin.renderInline(criteria.id)}
              </Box>
              <Divider />
            </Box>
          );
        })}
      </Stack>
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="baseline"
        sx={{ mt: 1 }}
      >
        <AddCriteriaButton group={group.id} />
        <Button
          variant="contained"
          disabled={cohort.groups.length === 1}
          onClick={() => {
            dispatch(
              deleteGroup({
                cohortId: cohort.id,
                groupId: group.id,
              })
            );
            const newIndex =
              groupIndex === cohort.groups.length - 1
                ? groupIndex - 1
                : groupIndex + 1;
            navigate("../" + cohortURL(cohort.id, cohort.groups[newIndex].id));
          }}
        >
          Delete Group
        </Button>
      </Stack>
    </Box>
  );
}

function AddCriteriaButton(props: { group: string }) {
  const underlay = useUnderlay();
  const source = useSource();
  const cohort = useCohort();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const configs = underlay.uiConfiguration.criteriaConfigs;

  const onAddCriteria = (criteria: tanagra.Criteria) => {
    dispatch(
      insertCriteria({ cohortId: cohort.id, groupId: props.group, criteria })
    );
    navigate(
      !!getCriteriaPlugin(criteria).renderEdit
        ? criteriaURL(criteria.id)
        : "../" + cohortURL(cohort.id, props.group)
    );
  };

  const [menu, show] = useMenu({
    children: configs.map((config) => (
      <MenuItem
        key={config.title}
        onClick={() => {
          onAddCriteria(createCriteria(source, config));
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
