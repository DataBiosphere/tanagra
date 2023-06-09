import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import LoopIcon from "@mui/icons-material/Loop";
import TuneIcon from "@mui/icons-material/Tune";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  deleteCohortCriteriaModifier,
  deleteCohortGroup,
  deleteCohortGroupSection,
  insertCohortCriteriaModifier,
  insertCohortGroupSection,
  updateCohort,
  updateCohortGroupSection,
  useCohortContext,
} from "cohortContext";
import CohortToolbar from "cohortToolbar";
import Empty from "components/empty";
import Loading from "components/loading";
import { useMenu } from "components/menu";
import { useTextInputDialog } from "components/textInputDialog";
import { useSource } from "data/source";
import { DemographicCharts } from "demographicCharts";
import { useCohort, useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useCallback, useMemo } from "react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import {
  absoluteCohortReviewListURL,
  cohortURL,
  criteriaURL,
  exitURL,
  useBaseParams,
} from "router";
import { StudyName } from "studyName";
import useSWRImmutable from "swr/immutable";
import * as tanagra from "tanagra-api";
import {
  createCriteria,
  defaultFilter,
  generateCohortFilter,
  getCriteriaPlugin,
  getCriteriaTitle,
  sectionName,
} from "./cohort";

export function Overview() {
  const context = useCohortContext();
  const cohort = useCohort();
  const params = useBaseParams();

  const [renameTitleDialog, showRenameTitleDialog] = useTextInputDialog({
    title: "Editing cohort name",
    initialText: cohort.name,
    textLabel: "Cohort name",
    buttonLabel: "Update",
    onConfirm: (name: string) => {
      updateCohort(context, name);
    },
  });

  return (
    <GridLayout rows>
      <ActionBar
        title={cohort.name}
        subtitle={
          <GridLayout cols spacing={1} rowAlign="middle">
            <StudyName />
            <Typography variant="body1">•</Typography>
            <SaveStatus />
          </GridLayout>
        }
        titleControls={
          <IconButton onClick={() => showRenameTitleDialog()}>
            <EditIcon />
          </IconButton>
        }
        rightControls={
          <Button
            variant="outlined"
            size="large"
            component={RouterLink}
            to={absoluteCohortReviewListURL(params, cohort.id)}
          >
            Review cohort
          </Button>
        }
        backURL={exitURL(params)}
      />
      <GridLayout
        rows="minmax(max-content, 100%)"
        cols="auto 380px"
        spacing={2}
        sx={{ px: 5, overflowY: "auto" }}
      >
        <GroupList />
        <GridBox
          sx={{
            alignSelf: "start",
            position: "sticky",
            top: 0,
            height: "auto",
          }}
        >
          <DemographicCharts cohort={cohort} />
          {renameTitleDialog}
        </GridBox>
      </GridLayout>
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

  return (
    <GridBox sx={{ pb: 2 }}>
      <GridLayout rows height="auto">
        <GridBox sx={{ py: 3 }}>
          <GridLayout cols fillCol={1} rowAlign="middle">
            <Typography variant="h6">
              To be included in the cohort, participants…
            </Typography>
            <GridBox />
            <CohortToolbar />
          </GridLayout>
        </GridBox>
        <GridLayout rows spacing={2} height="auto">
          {cohort.groupSections.map((s, index) => (
            <GridLayout key={s.id} rows spacing={2} height="auto">
              {index !== 0 ? <GroupDivider /> : null}
              <GridBox>
                <ParticipantsGroupSection
                  groupSection={s}
                  sectionIndex={index}
                />
              </GridBox>
            </GridLayout>
          ))}
          {cohort.groupSections.length > 1 ||
          cohort.groupSections[0].groups.length > 0 ? (
            <GridLayout rows spacing={2} height="auto">
              <GroupDivider />
              <Button
                onClick={() => insertCohortGroupSection(context)}
                variant="contained"
              >
                Add group
              </Button>
            </GridLayout>
          ) : null}
        </GridLayout>
      </GridLayout>
    </GridBox>
  );
}

function ParticipantsGroupSection(props: {
  groupSection: tanagra.GroupSection;
  sectionIndex: number;
}) {
  const source = useSource();
  const cohort = useCohort();
  const context = useCohortContext();
  const navigate = useNavigate();

  const fetchSectionCount = useCallback(async () => {
    const cohortForFilter: tanagra.Cohort = {
      ...cohort,
      groupSections: [props.groupSection],
    };

    const filter = generateCohortFilter(cohortForFilter);
    if (!filter) {
      return 0;
    }

    return (await source.filterCount(filter))[0].count;
  }, [cohort.underlayName, props.sectionIndex, props.groupSection]);

  const sectionCountState = useSWRImmutable(
    {
      component: "Overview",
      cohortId: cohort.id,
      cohortName: cohort.name,
      underlayName: cohort.underlayName,
      groupSections: props.groupSection,
    },
    fetchSectionCount
  );
  const name = sectionName(props.groupSection, props.sectionIndex);

  const [renameGroupDialog, showRenameGroup] = useTextInputDialog({
    title: "Editing group name",
    initialText: name,
    textLabel: "Group name",
    buttonLabel: "Update",
    onConfirm: (name: string) => {
      updateCohortGroupSection(context, props.groupSection.id, name);
    },
  });

  return (
    <Paper sx={{ width: "100%", overflow: "hidden" }}>
      <GridLayout rows height="auto">
        <GridLayout
          rows
          spacing={1}
          height="auto"
          sx={{ p: 2, backgroundColor: (theme) => theme.palette.info.main }}
        >
          <GridLayout cols fillCol={2} rowAlign="middle">
            <Typography variant="body1em" sx={{ mr: 1 }}>
              {name}
            </Typography>
            <IconButton onClick={showRenameGroup}>
              <EditIcon />
            </IconButton>
            <GridBox />
            <IconButton
              onClick={() => {
                deleteCohortGroupSection(context, props.groupSection.id);
              }}
              sx={{ mr: -1 }}
            >
              <DeleteIcon />
            </IconButton>
          </GridLayout>
          <GridLayout cols fillCol={4} rowAlign="baseline">
            <FormControl>
              <Select
                value={props.groupSection.filter.excluded ? 1 : 0}
                onChange={(event: SelectChangeEvent<number>) => {
                  updateCohortGroupSection(
                    context,
                    props.groupSection.id,
                    undefined,
                    {
                      ...props.groupSection.filter,
                      excluded: event.target.value === 1,
                    }
                  );
                }}
                sx={{
                  color: (theme) => theme.palette.primary.main,
                  "& .MuiOutlinedInput-notchedOutline": {
                    borderColor: (theme) => theme.palette.primary.main,
                  },
                }}
              >
                <MenuItem value={0}>Must</MenuItem>
                <MenuItem value={1}>Must not</MenuItem>
              </Select>
            </FormControl>
            <Typography variant="body1">&nbsp;meet&nbsp;</Typography>
            <FormControl>
              <Select
                value={props.groupSection.filter.kind}
                onChange={(event: SelectChangeEvent<string>) => {
                  updateCohortGroupSection(
                    context,
                    props.groupSection.id,
                    undefined,
                    {
                      ...props.groupSection.filter,
                      kind: event.target
                        .value as tanagra.GroupSectionFilterKindEnum,
                    }
                  );
                }}
                sx={{
                  color: (theme) => theme.palette.primary.main,
                  "& .MuiOutlinedInput-notchedOutline": {
                    borderColor: (theme) => theme.palette.primary.main,
                  },
                }}
              >
                <MenuItem value={tanagra.GroupSectionFilterKindEnum.Any}>
                  any
                </MenuItem>
                <MenuItem value={tanagra.GroupSectionFilterKindEnum.All}>
                  all
                </MenuItem>
              </Select>
            </FormControl>
            <Typography variant="body1">
              &nbsp;of the following criteria
            </Typography>
            <GridBox />
            <Typography variant="body1" color="text.muted">
              Group count:&nbsp;
            </Typography>
            <Loading status={sectionCountState} size="small">
              <Typography variant="body1" color="text.muted">
                {sectionCountState.data?.toLocaleString()}
              </Typography>
            </Loading>
          </GridLayout>
        </GridLayout>
        <GridLayout rows height="auto" sx={{ p: 2 }}>
          {props.groupSection.groups.length === 0 ? (
            <Empty
              maxWidth="90%"
              minHeight="60px"
              title="No criteria yet"
              subtitle="You can add a criteria by clicking on 'Add criteria'"
            />
          ) : (
            props.groupSection.groups.map((group) => (
              <GridLayout key={group.id} rows height="auto">
                <Box>
                  <ParticipantsGroup
                    groupSection={props.groupSection}
                    group={group}
                  />
                </Box>
                <Divider sx={{ my: 2 }} />
              </GridLayout>
            ))
          )}
          <Button
            onClick={() =>
              navigate(`../${cohortURL(cohort.id, props.groupSection.id)}/add`)
            }
            variant="contained"
          >
            Add criteria
          </Button>
        </GridLayout>
      </GridLayout>
      {renameGroupDialog}
    </Paper>
  );
}

function ParticipantsGroup(props: {
  groupSection: tanagra.GroupSection;
  group: tanagra.Group;
}) {
  const source = useSource();
  const cohort = useCohort();
  const navigate = useNavigate();
  const context = useCohortContext();
  const uiConfig = useUnderlay().uiConfiguration;

  const fetchGroupCount = useCallback(async () => {
    const cohortForFilter: tanagra.Cohort = {
      ...cohort,
      groupSections: [
        {
          id: props.groupSection.id,
          filter: defaultFilter,
          groups: [props.group],
        },
      ],
    };

    const filter = generateCohortFilter(cohortForFilter);
    if (!filter) {
      return 0;
    }

    return (await source.filterCount(filter))[0].count;
  }, [cohort.underlayName, props.group]);

  const groupCountState = useSWRImmutable(
    {
      component: "Overview",
      cohortId: cohort.id,
      cohortName: cohort.name,
      underlayName: cohort.underlayName,
      criteria: props.group,
    },
    fetchGroupCount
  );

  const plugin = getCriteriaPlugin(props.group.criteria[0]);
  const title = getCriteriaTitle(props.group.criteria[0], plugin);

  const modifierCriteria = useMemo(
    () => props.group.criteria.slice(1),
    [props.group.criteria]
  );

  const modifierPlugins = useMemo(
    () => modifierCriteria.map((c) => getCriteriaPlugin(c, props.group.entity)),
    [modifierCriteria, props.group.entity]
  );

  const hasModifiers = props.group.criteria[0].config.modifiers?.length;
  const [menu, showMenu] = useMenu({
    children: props.group.criteria[0].config.modifiers?.map((m: string) => {
      const config = uiConfig.modifierConfigs?.find((c) => c.id === m);
      if (!config) {
        return null;
      }

      const sel = !!modifierCriteria.find((c) => c.config.id === m);

      return (
        <MenuItem
          key={m}
          selected={sel}
          disabled={sel}
          onClick={() => {
            insertCohortCriteriaModifier(
              context,
              props.groupSection.id,
              props.group.id,
              createCriteria(source, config)
            );
          }}
        >
          {config.title}
        </MenuItem>
      );
    }),
  });

  const inline = plugin.renderInline(props.group.id);

  return (
    <GridLayout key={props.group.id} rows height="auto">
      <GridLayout cols fillCol={4} height="auto" rowAlign="middle">
        <Typography variant="body1" sx={{ pr: 1 }}>
          {title}
        </Typography>
        {!!plugin.renderEdit ? (
          <IconButton
            data-testid={title}
            onClick={() => navigate(criteriaURL(props.group.id))}
          >
            <EditIcon fontSize="small" />
          </IconButton>
        ) : (
          <GridBox />
        )}
        <IconButton
          onClick={() => {
            deleteCohortGroup(context, props.groupSection.id, props.group.id);
          }}
        >
          <DeleteIcon fontSize="small" />
        </IconButton>
        {hasModifiers ? (
          <Button startIcon={<TuneIcon fontSize="small" />} onClick={showMenu}>
            Modifiers
          </Button>
        ) : (
          <GridBox />
        )}
        <GridBox />
        <Loading status={groupCountState} size="small">
          <Typography variant="body2" color="text.muted" sx={{ ml: 1 }}>
            {groupCountState.data?.toLocaleString()}
          </Typography>
        </Loading>
      </GridLayout>
      {inline ? <GridBox sx={{ pt: 2 }}>{inline}</GridBox> : null}
      {modifierPlugins.length > 0 ? (
        <GridLayout rows height="auto" sx={{ p: 1 }}>
          {modifierPlugins.map((p, i) => (
            <GridLayout key={p.id} rows height="auto">
              <GridBox
                sx={{
                  boxShadow: (theme) =>
                    `inset 1px 0 0 ${theme.palette.divider}`,
                  height: (theme) => theme.spacing(1),
                }}
              />
              <GridLayout cols="16px 1fr" spacing={1} height="auto">
                <GridLayout rows="1fr 1fr">
                  <GridBox
                    sx={{
                      boxShadow: (theme) =>
                        `inset 1px -1px 0 ${theme.palette.divider}`,
                    }}
                  />
                  <GridBox
                    sx={{
                      boxShadow: (theme) =>
                        i !== modifierPlugins.length - 1
                          ? `inset 1px 0 0 ${theme.palette.divider}`
                          : undefined,
                    }}
                  />
                </GridLayout>
                <GridLayout cols height="auto" rowAlign="middle">
                  <Typography variant="body2">
                    {modifierCriteria[i].config.title}
                  </Typography>
                  <IconButton
                    onClick={() =>
                      deleteCohortCriteriaModifier(
                        context,
                        props.groupSection.id,
                        props.group.id,
                        p.id
                      )
                    }
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </GridLayout>
              </GridLayout>
              <GridLayout key={p.id} cols="24px 1fr" height="auto">
                <GridBox
                  sx={{
                    boxShadow: (theme) =>
                      i !== modifierPlugins.length - 1
                        ? `inset 1px 0 0 ${theme.palette.divider}`
                        : undefined,
                  }}
                />
                <GridBox key={p.id} sx={{ height: "auto" }}>
                  {p.renderInline(props.group.id)}
                </GridBox>
              </GridLayout>
            </GridLayout>
          ))}
        </GridLayout>
      ) : null}
      {menu}
    </GridLayout>
  );
}

function SaveStatus() {
  const context = useCohortContext();
  if (!context?.state) {
    return null;
  }

  return (
    <GridLayout cols rowAlign="middle">
      {!context.state.saving ? (
        <LoopIcon fontSize="small" sx={{ display: "block" }} />
      ) : null}
      <Typography variant="body1">
        {context.state.saving
          ? "Saving..."
          : `Last saved: ${context.state.present.lastModified.toLocaleString()}`}
      </Typography>
    </GridLayout>
  );
}
