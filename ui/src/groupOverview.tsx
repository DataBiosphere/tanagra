import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputLabel from "@mui/material/InputLabel";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  deleteCriteria,
  deleteGroup,
  renameGroup,
  setGroupFilter,
} from "cohortsSlice";
import CohortToolbar from "cohortToolbar";
import Empty from "components/empty";
import { useTextInputDialog } from "components/textInputDialog";
import { useAppDispatch, useCohortAndGroup } from "hooks";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { cohortURL, criteriaURL } from "router";
import {
  filterWithUIGroupFilter,
  getCriteriaPlugin,
  getCriteriaTitle,
  groupName,
  UIGroupFilter,
  uiGroupFilterFromFilter,
} from "./cohort";

export function GroupOverview() {
  const { cohort, group, groupIndex } = useCohortAndGroup();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const name = groupName(group, groupIndex);

  const [renameGroupDialog, showRenameGroup] = useTextInputDialog({
    title: "Edit Group Name",
    initialText: name,
    textLabel: "Group name",
    buttonLabel: "Rename group",
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
      <ActionBar title={cohort.name} extraControls={<CohortToolbar />} />
      <Stack
        direction="row"
        alignItems="center"
        sx={{ height: (theme) => theme.spacing(6) }}
      >
        <Typography variant="h2" sx={{ mr: 1 }}>
          {name}
        </Typography>
        <IconButton onClick={showRenameGroup}>
          <EditIcon />
        </IconButton>
        {renameGroupDialog}
        <IconButton
          onClick={() => {
            const action = dispatch(deleteGroup(cohort, group.id));
            navigate("../" + cohortURL(cohort.id, action.payload.nextGroupId));
          }}
        >
          <DeleteIcon />
        </IconButton>
        <Divider
          orientation="vertical"
          variant="middle"
          flexItem
          sx={{ mr: 1 }}
        />
        <FormControl>
          <InputLabel id="group-kind-label">Require</InputLabel>
          <Select
            value={uiGroupFilterFromFilter(group.filter)}
            label="Require"
            onChange={(event: SelectChangeEvent<string>) => {
              dispatch(
                setGroupFilter(
                  cohort.id,
                  group.id,
                  filterWithUIGroupFilter(
                    group.filter,
                    event.target.value as UIGroupFilter
                  )
                )
              );
            }}
          >
            {Object.values(UIGroupFilter).map((value) => (
              <MenuItem key={value} value={value}>
                {value}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
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
                  <Stack direction="row" alignItems="flex-start">
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
                      <DeleteIcon fontSize="small" sx={{ mt: "-3px" }} />
                    </IconButton>
                  </Stack>
                  {plugin.renderInline(criteria.id)}
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
