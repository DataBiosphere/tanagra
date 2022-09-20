import CheckIcon from "@mui/icons-material/Check";
import ClearIcon from "@mui/icons-material/Clear";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  deleteCriteria,
  deleteGroup,
  renameGroup,
  setGroupKind,
} from "cohortsSlice";
import { useTextInputDialog } from "components/textInputDialog";
import { useAppDispatch, useCohortAndGroup } from "hooks";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { cohortURL, criteriaURL } from "router";
import * as tanagra from "tanagra-api";
import { getCriteriaPlugin, getCriteriaTitle, groupName } from "./cohort";

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
          <IconButton
            onClick={() => {
              const action = dispatch(deleteGroup(cohort, group.id));
              navigate(
                "../" + cohortURL(cohort.id, action.payload.nextGroupId)
              );
            }}
          >
            <DeleteIcon />
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
          const title = getCriteriaTitle(criteria, plugin);

          return (
            <Box key={criteria.id}>
              <Stack
                direction="row"
                justifyContent="space-between"
                alignItems="baseline"
                sx={{ m: 1 }}
              >
                <Box>
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
                  {plugin.renderInline(criteria.id)}
                </Box>
                <IconButton
                  onClick={() => {
                    dispatch(
                      deleteCriteria({
                        cohortId: cohort.id,
                        groupId: group.id,
                        criteriaId: criteria.id,
                      })
                    );
                  }}
                >
                  <DeleteIcon />
                </IconButton>
              </Stack>
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
        <Button
          onClick={() => navigate("add")}
          variant="contained"
          className="add-criteria"
        >
          Add Criteria
        </Button>
      </Stack>
    </Box>
  );
}
