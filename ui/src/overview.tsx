import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import TuneIcon from "@mui/icons-material/Tune";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
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
import Empty from "components/empty";
import Loading from "components/loading";
import { useMenu } from "components/menu";
import { SaveStatus } from "components/saveStatus";
import { useTextInputDialog } from "components/textInputDialog";
import {
  Cohort,
  Group,
  GroupSection,
  GroupSectionFilterKind,
} from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { DemographicCharts } from "demographicCharts";
import { useCohort, useCohortGroupSectionAndGroup, useStudyId } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { ReactNode, useCallback, useMemo } from "react";
import { useParams } from "react-router-dom";
import {
  absoluteCohortReviewListURL,
  cohortURL,
  criteriaURL,
  useBaseParams,
  useExitAction,
} from "router";
import { StudyName } from "studyName";
import useSWRImmutable from "swr/immutable";
import UndoRedoToolbar from "undoRedoToolbar";
import { RouterLink, useNavigate } from "util/searchState";
import {
  createCriteria,
  defaultFilter,
  generateCohortFilter,
  getCriteriaPlugin,
  getCriteriaTitle,
} from "./cohort";

export function Overview() {
  const context = useCohortContext();
  const cohort = useCohort();
  const params = useBaseParams();
  const exit = useExitAction();

  const [renameTitleDialog, showRenameTitleDialog] = useTextInputDialog();

  return (
    <GridLayout rows>
      <ActionBar
        title={cohort.name}
        subtitle={
          <GridLayout cols spacing={5} rowAlign="middle">
            <StudyName />
            <SaveStatus
              saving={context.state?.saving}
              lastModified={context.state?.present?.lastModified}
            />
          </GridLayout>
        }
        titleControls={
          <IconButton
            onClick={() =>
              showRenameTitleDialog({
                title: "Editing cohort name",
                initialText: cohort.name,
                textLabel: "Cohort name",
                buttonLabel: "Update",
                onConfirm: (name: string) => {
                  updateCohort(context, name);
                },
              })
            }
          >
            <EditIcon />
          </IconButton>
        }
        rightControls={<UndoRedoToolbar />}
        backAction={exit}
      />
      <GridLayout
        rows="minmax(max-content, 100%)"
        cols="auto 380px"
        spacing={2}
        sx={{ px: 5, overflowY: "auto" }}
      >
        <GridBox sx={{ pt: 3 }}>
          <GroupList />
        </GridBox>
        <GridBox
          sx={{
            pt: 3,
            alignSelf: "start",
            position: "sticky",
            top: 0,
            height: "auto",
          }}
        >
          <DemographicCharts
            cohort={cohort}
            extraControls={
              <Button
                variant="outlined"
                size="large"
                component={RouterLink}
                to={absoluteCohortReviewListURL(params, cohort.id)}
              >
                Review individuals
              </Button>
            }
          />
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
      <GridLayout rows spacing={2} height="auto">
        {cohort.groupSections.map((s, index) => (
          <GridLayout key={s.id} rows spacing={2} height="auto">
            {index !== 0 ? <GroupDivider /> : null}
            <GridBox>
              <ParticipantsGroupSection groupSection={s} sectionIndex={index} />
            </GridBox>
          </GridLayout>
        ))}
        {cohort.groupSections.length > 1 ||
        cohort.groupSections[0].groups.length > 0 ? (
          <GridLayout rows spacing={2} height="auto">
            <GroupDivider />
            <Paper
              sx={{
                width: "100%",
                overflow: "hidden",
                backgroundColor: "unset",
                borderStyle: "dashed",
                borderWidth: "1px",
                borderColor: (theme) => theme.palette.divider,
                p: 2,
              }}
            >
              <GridLayout cols colAlign="center">
                <GridBox>
                  <Link
                    variant="link"
                    underline="hover"
                    onClick={() => insertCohortGroupSection(context)}
                    sx={{ cursor: "pointer" }}
                  >
                    Add another group
                  </Link>{" "}
                  to manage a new set of criteria
                </GridBox>
              </GridLayout>
            </Paper>
          </GridLayout>
        ) : null}
      </GridLayout>
    </GridBox>
  );
}

function ParticipantsGroupSection(props: {
  groupSection: GroupSection;
  sectionIndex: number;
}) {
  const studyId = useStudyId();
  const studySource = useStudySource();
  const underlaySource = useUnderlaySource();
  const cohort = useCohort();
  const context = useCohortContext();
  const navigate = useNavigate();
  const { group } = useCohortGroupSectionAndGroup();

  const fetchSectionCount = useCallback(async () => {
    if (!props.groupSection.groups.length) {
      return -1;
    }

    if (process.env.REACT_APP_BACKEND_FILTERS) {
      return (
        await studySource.cohortCount(studyId, cohort.id, props.groupSection.id)
      )[0].count;
    }
    const cohortForFilter: Cohort = {
      ...cohort,
      groupSections: [props.groupSection],
    };

    const filter = generateCohortFilter(underlaySource, cohortForFilter);
    return (await underlaySource.filterCount(filter))[0].count;
  }, [studyId, cohort.id, cohort.underlayName, props.groupSection]);

  const sectionCountState = useSWRImmutable(
    {
      component: "Overview",
      studyId,
      cohortId: cohort.id,
      underlayName: cohort.underlayName,
      groupSection: props.groupSection,
    },
    fetchSectionCount
  );

  return (
    <Paper sx={{ width: "100%", overflow: "hidden", pb: 2 }}>
      <GridLayout rows height="auto">
        <GridLayout
          cols
          fillCol={4}
          rowAlign="baseline"
          sx={{ p: 2, backgroundColor: (theme) => theme.palette.info.main }}
        >
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
                "& .MuiOutlinedInput-input": {
                  py: "2px",
                },
                "& .MuiOutlinedInput-notchedOutline": {
                  borderColor: (theme) => theme.palette.primary.main,
                },
              }}
            >
              <MenuItem value={0}>Include</MenuItem>
              <MenuItem value={1}>Exclude</MenuItem>
            </Select>
          </FormControl>
          <Typography variant="body1">
            &nbsp;participants who meet&nbsp;
          </Typography>
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
                    kind: event.target.value as GroupSectionFilterKind,
                  }
                );
              }}
              sx={{
                color: (theme) => theme.palette.primary.main,
                "& .MuiOutlinedInput-input": {
                  py: "2px",
                },
                "& .MuiOutlinedInput-notchedOutline": {
                  borderColor: (theme) => theme.palette.primary.main,
                },
              }}
            >
              <MenuItem value={GroupSectionFilterKind.Any}>any</MenuItem>
              <MenuItem value={GroupSectionFilterKind.All}>all</MenuItem>
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
              {(sectionCountState.data ?? -1) < 0
                ? "-"
                : sectionCountState.data?.toLocaleString()}
            </Typography>
          </Loading>
        </GridLayout>
      </GridLayout>
      <GridLayout rows height="auto">
        {props.groupSection.groups.length === 0 ? (
          <GridBox sx={{ pt: 2 }}>
            <Empty
              maxWidth="90%"
              minHeight="60px"
              title="Criteria are traits you select to define your cohort’s participant groups"
              subtitle={
                <>
                  <Link
                    variant="link"
                    underline="hover"
                    onClick={() =>
                      navigate(
                        `../${cohortURL(cohort.id, props.groupSection.id)}/add`
                      )
                    }
                    sx={{ cursor: "pointer" }}
                  >
                    Add some criteria
                  </Link>{" "}
                  to this group to get started
                </>
              }
            />
          </GridBox>
        ) : (
          props.groupSection.groups.map((group) => (
            <GridLayout key={group.id} rows height="auto">
              <Box>
                <ParticipantsGroup
                  groupSection={props.groupSection}
                  group={group}
                />
              </Box>
              <Divider variant="middle">
                <Chip
                  label={
                    props.groupSection.filter.kind ===
                    GroupSectionFilterKind.Any
                      ? "OR"
                      : "AND"
                  }
                />
              </Divider>
            </GridLayout>
          ))
        )}
        {props.groupSection.groups.length !== 0 ||
        cohort.groupSections.length > 1 ? (
          <GridLayout cols fillCol={1} height="auto" sx={{ px: 2 }}>
            <Button
              onClick={() =>
                navigate(
                  `../${cohortURL(
                    cohort.id,
                    props.groupSection.id,
                    group?.id
                  )}/add`
                )
              }
              variant="contained"
              sx={{
                display:
                  props.groupSection.groups.length === 0 ? "hidden" : undefined,
              }}
            >
              Add criteria
            </Button>
            <GridBox />
            <Button
              color="error"
              variant="outlined"
              startIcon={<DeleteIcon />}
              onClick={() => {
                deleteCohortGroupSection(context, props.groupSection.id);
              }}
            >
              Delete group
            </Button>
          </GridLayout>
        ) : null}
      </GridLayout>
    </Paper>
  );
}

function ParticipantsGroup(props: {
  groupSection: GroupSection;
  group: Group;
}) {
  const studyId = useStudyId();
  const studySource = useStudySource();
  const underlaySource = useUnderlaySource();
  const cohort = useCohort();
  const navigate = useNavigate();
  const context = useCohortContext();
  const { groupId } = useParams<{ groupId: string }>();

  const fetchGroupCount = useCallback(async () => {
    if (process.env.REACT_APP_BACKEND_FILTERS) {
      return (
        await studySource.cohortCount(
          studyId,
          cohort.id,
          props.groupSection.id,
          props.group.id
        )
      )[0].count;
    }
    const cohortForFilter: Cohort = {
      ...cohort,
      groupSections: [
        {
          id: props.groupSection.id,
          filter: defaultFilter,
          groups: [props.group],
        },
      ],
    };

    const filter = generateCohortFilter(underlaySource, cohortForFilter);
    return (await underlaySource.filterCount(filter))[0].count;
  }, [
    studyId,
    cohort.id,
    cohort.underlayName,
    props.groupSection,
    props.group,
  ]);

  const groupCountState = useSWRImmutable(
    {
      component: "Overview",
      studyId,
      cohortId: cohort.id,
      underlayName: cohort.underlayName,
      group: props.group,
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
    () =>
      modifierCriteria.map((c) => {
        // TODO: Multiple occurrence: This assumes modifiers can apply to all
        // occurrence entities. There's no way around this without rethinking
        // how modifiers work.
        const p = getCriteriaPlugin(
          c,
          plugin.filterEntityIds(underlaySource)[0]
        );
        return { title: getCriteriaTitle(c, p), plugin: p };
      }),
    [modifierCriteria, props.group.entity]
  );

  const selector = underlaySource.lookupCriteriaSelector(
    props.group.criteria[0].config.name
  );
  const hasModifiers = selector.modifiers?.length;

  const [menu, showMenu] = useMenu({
    children: selector.modifiers?.map((config) => {
      const sel = !!modifierCriteria.find((c) => c.config.name === config.name);

      return (
        <MenuItem
          key={config.name}
          selected={sel}
          disabled={sel}
          onClick={() => {
            insertCohortCriteriaModifier(
              context,
              props.groupSection.id,
              props.group.id,
              createCriteria(underlaySource, config)
            );
          }}
        >
          {config.displayName}
        </MenuItem>
      );
    }),
  });

  const selected = groupId === props.group.id;
  const additionalText = plugin.displayDetails().additionalText;
  let inline: ReactNode = null;
  if (selected) {
    inline = plugin.renderInline(props.group.id);
    if (inline) {
      inline = <GridBox sx={{ pt: 2 }}>{inline}</GridBox>;
    } else if (additionalText) {
      inline = (
        <Typography variant="body2">
          {plugin.displayDetails().additionalText?.join(", ")}
        </Typography>
      );
    }
  }

  return (
    <GridBox key={props.group.id} sx={{ height: "auto" }}>
      <GridBox
        onClick={() => {
          navigate(
            "../" +
              cohortURL(
                cohort.id,
                props.groupSection.id,
                !selected ? props.group.id : undefined
              )
          );
        }}
        sx={{
          p: 2,
          height: "auto",
          ...(selected
            ? {
                backgroundColor: "#F1F2FA",
                boxShadow: "inset 0 -1px 0 #BEC2E9, inset 0 1px 0 #BEC2E9",
              }
            : {
                "&:hover": {
                  textDecoration: "underline",
                  cursor: "pointer",
                  color: (theme) =>
                    (theme.typography as { link: { color: string } }).link
                      .color,
                },
              }),
        }}
      >
        <GridLayout key={props.group.id} rows height="auto">
          <GridLayout cols fillCol={2} height="auto" rowAlign="middle">
            <GridBox
              sx={{
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
                overflow: "hidden",
              }}
            >
              <Typography
                variant="body1"
                title={title}
                sx={{ display: "inline", pr: 1 }}
              >
                {title}
              </Typography>
            </GridBox>
            <GridBox
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
              }}
              sx={{ visibility: selected ? "visible" : "hidden" }}
            >
              {!!plugin.renderEdit ? (
                <IconButton
                  data-testid={title}
                  onClick={() => navigate(criteriaURL())}
                >
                  <EditIcon fontSize="small" />
                </IconButton>
              ) : (
                <GridBox />
              )}
              <IconButton
                onClick={() => {
                  deleteCohortGroup(
                    context,
                    props.groupSection.id,
                    props.group.id
                  );
                }}
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
              {hasModifiers ? (
                <Button
                  startIcon={<TuneIcon fontSize="small" />}
                  onClick={showMenu}
                >
                  Modifiers
                </Button>
              ) : undefined}
            </GridBox>
            <GridBox />
            <Loading status={groupCountState} size="small">
              <Typography
                variant="body2"
                color="text.muted"
                sx={{
                  display: "inline-block",
                  ml: 1,
                  "&:hover": { textDecoration: "none" },
                }}
              >
                {groupCountState.data?.toLocaleString()}
              </Typography>
            </Loading>
          </GridLayout>
          {inline}
          {modifierPlugins.length > 0 ? (
            <GridLayout rows height="auto" sx={{ px: 1 }}>
              {modifierPlugins.map(({ plugin: p, title: t }, i) =>
                selected ? (
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
                          {modifierCriteria[i].config.displayName}
                        </Typography>
                        <GridBox
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                          }}
                        >
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
                        </GridBox>
                        <GridBox />
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
                ) : (
                  <GridBox sx={{ pl: 4 }}>
                    <Typography variant="body2">{t}</Typography>
                  </GridBox>
                )
              )}
            </GridLayout>
          ) : null}
        </GridLayout>
      </GridBox>
      {menu}
    </GridBox>
  );
}
