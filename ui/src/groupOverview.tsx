import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputLabel from "@mui/material/InputLabel";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  deleteCriteria,
  deleteGroup,
  renameGroup,
  setGroupFilter,
} from "cohortsSlice";
import Empty from "components/empty";
import { useTextInputDialog } from "components/textInputDialog";
import UndoRedo from "components/UndoRedo";
import { useAppDispatch, useCohortAndGroup } from "hooks";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { cohortURL, criteriaURL } from "router";
import * as tanagra from "tanagra-api";
import {
  getCriteriaPlugin,
  getCriteriaTitle,
  groupFilterKindLabel,
  groupName,
} from "./cohort";

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
    <Box sx={{ p: 1 }}>
      <ActionBar title={cohort.name} extraControls={<UndoRedo />} />
      <Stack direction="row" justifyContent="space-between">
        <Stack direction="row" alignItems="center">
          <Typography variant="h2">{name}</Typography>
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
          <FormControl>
            <InputLabel id="group-kind-label">Require</InputLabel>
            <Select
              value={group.filter.kind}
              label="Require"
              onChange={(event: SelectChangeEvent<string>) => {
                dispatch(
                  setGroupFilter(cohort.id, group.id, {
                    ...group.filter,
                    kind: event.target.value as tanagra.GroupFilterKindEnum,
                  })
                );
              }}
            >
              {Object.values(tanagra.GroupFilterKindEnum).map((value) => (
                <MenuItem key={value} value={value}>
                  {groupFilterKindLabel(value)}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Switch
            checked={group.filter.excluded}
            onChange={() =>
              dispatch(
                setGroupFilter(cohort.id, group.id, {
                  ...group.filter,
                  excluded: !group.filter.excluded,
                })
              )
            }
            inputProps={{ "aria-label": "controlled" }}
          />
          <Typography>Excluded</Typography>
        </Stack>
      </Stack>
      <Stack spacing={1}>
        {group.criteria.length === 0 && (
          <Empty
            minHeight="300px"
            image="/empty.png"
            title="No criteria yet"
            subtitle="You can add a criteria by clicking on 'Add criteria'"
          />
        )}
        {group.criteria.length > 0 &&
          group.criteria.map((criteria) => {
            const plugin = getCriteriaPlugin(criteria);
            const title = getCriteriaTitle(criteria, plugin);

            return (
              <Box key={criteria.id}>
                <Paper sx={{ p: 1 }}>
                  <Stack
                    direction="row"
                    justifyContent="space-between"
                    alignItems="flex-start"
                  >
                    <Box>
                      {!!plugin.renderEdit ? (
                        <Link
                          variant="h4"
                          color="inherit"
                          underline="hover"
                          component={RouterLink}
                          to={criteriaURL(criteria.id)}
                        >
                          {title}
                        </Link>
                      ) : (
                        <Typography variant="h4">{title}</Typography>
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
                </Paper>
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
          Add criteria
        </Button>
      </Stack>
    </Box>
  );
}
