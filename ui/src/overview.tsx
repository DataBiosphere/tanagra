import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutline";
import TuneIcon from "@mui/icons-material/Tune";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import TextField from "@mui/material/TextField";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { createCriteria, getCriteriaPlugin, getCriteriaTitle } from "cohort";
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
  Group,
  GroupSection,
  GroupSectionFilterKind,
  GroupSectionReducingOperator,
  isTemporalSection,
} from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { DemographicCharts } from "demographicCharts";
import {
  useBackendCohort,
  useCohort,
  useCohortGroupSectionAndGroup,
  useStudyId,
} from "hooks";
import { GridBox, GridBoxPaper } from "layout/gridBox";
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
import { isValid } from "util/valid";

export function Overview() {
  const context = useCohortContext();
  const cohort = useCohort();
  const backendCohort = useBackendCohort();
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
        cols="minmax(440px, auto) 450px"
        spacing={2}
        sx={{ px: 5, overflow: "auto" }}
      >
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
            cohort={backendCohort}
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
        <GridBox sx={{ pt: 3 }}>
          <Paper
            sx={{
              p: 2,
            }}
          >
            <GroupList />
          </Paper>
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
    <GridLayout rows spacing={2} height="auto">
      <Typography variant="h6">Cohort filter</Typography>
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
  const backendCohort = useBackendCohort();
  const context = useCohortContext();
  const navigate = useNavigate();
  const { group } = useCohortGroupSectionAndGroup();

  const backendGroupSection = useMemo(
    () =>
      backendCohort.groupSections.find((gs) => gs.id === props.groupSection.id),
    [backendCohort, props.groupSection]
  );

  const fetchSectionCount = useCallback(async () => {
    if (!backendGroupSection?.groups?.length) {
      return -1;
    }

    return (
      await studySource.cohortCount(studyId, cohort.id, backendGroupSection.id)
    )[0].count;
  }, [studyId, cohort.id, cohort.underlayName, backendGroupSection]);

  const sectionCountState = useSWRImmutable(
    {
      component: "Overview",
      studyId,
      cohortId: cohort.id,
      underlayName: cohort.underlayName,
      groupSection: backendGroupSection,
    },
    fetchSectionCount
  );

  const showBlocks = isTemporalSection(props.groupSection);
  const combinedGroups = [
    ...props.groupSection.groups,
    ...props.groupSection.secondBlockGroups,
  ];
  const firstBlockGroups = !showBlocks
    ? combinedGroups
    : props.groupSection.groups;

  return (
    <GridBoxPaper
      sx={{
        borderWidth: "1px",
        borderColor: (theme) => theme.palette.divider,
        borderStyle: "solid",
      }}
    >
      <GridLayout rows height="auto">
        <GridLayout
          cols
          fillCol={3}
          rowAlign="baseline"
          sx={{ p: 2, backgroundColor: (theme) => theme.palette.info.main }}
        >
          <FormControl>
            <Select
              value={props.groupSection.filter.excluded ? 1 : 0}
              onChange={(event: SelectChangeEvent<number>) => {
                updateCohortGroupSection(context, props.groupSection.id, {
                  filter: {
                    ...props.groupSection.filter,
                    excluded: event.target.value === 1,
                  },
                });
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
          <GridBox sx={{ width: (theme) => theme.spacing(1) }} />
          <FormControl>
            <Select
              value={props.groupSection.filter.kind}
              onChange={(event: SelectChangeEvent<string>) => {
                updateCohortGroupSection(context, props.groupSection.id, {
                  filter: {
                    ...props.groupSection.filter,
                    kind: event.target.value as GroupSectionFilterKind,
                  },
                });
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
              <MenuItem value={GroupSectionFilterKind.Any}>
                Meet any criteria
              </MenuItem>
              <MenuItem value={GroupSectionFilterKind.All}>
                Meet all criteria
              </MenuItem>
              {underlaySource.computedProperties().supportsTemporalQueries ? (
                <MenuItem value={GroupSectionFilterKind.SameEncounter}>
                  Same encounter as
                </MenuItem>
              ) : null}
              {underlaySource.computedProperties().supportsTemporalQueries ? (
                <MenuItem value={GroupSectionFilterKind.DaysBefore}>
                  X or more days before
                </MenuItem>
              ) : null}
              {underlaySource.computedProperties().supportsTemporalQueries ? (
                <MenuItem value={GroupSectionFilterKind.DaysAfter}>
                  X or more days after
                </MenuItem>
              ) : null}
              {underlaySource.computedProperties().supportsTemporalQueries ? (
                <MenuItem value={GroupSectionFilterKind.DaysWithin}>
                  Within X days of
                </MenuItem>
              ) : null}
            </Select>
          </FormControl>
          <GridBox />
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
        {showBlocks ? (
          <ReducingOperator groupSection={props.groupSection} />
        ) : null}
        {firstBlockGroups.length === 0 ? (
          !showBlocks ? (
            <Empty
              maxWidth="90%"
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
          ) : (
            <Empty
              maxWidth="90%"
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
                    Choose main criteria
                  </Link>{" "}
                  to compare against. Some criteria aren&apos;t eligible for
                  this filter.
                </>
              }
            />
          )
        ) : (
          firstBlockGroups.map((group) => (
            <ParticipantsGroup
              key={group.id}
              groupSection={props.groupSection}
              group={group}
            />
          ))
        )}
        {combinedGroups.length > 0 ? (
          <GridLayout
            cols
            colAlign="left"
            height="auto"
            sx={{ px: 2, pt: 1, pb: 2 }}
          >
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
            >
              Add criteria
            </Button>
          </GridLayout>
        ) : null}
        {showBlocks ? (
          <ReducingOperator groupSection={props.groupSection} second />
        ) : null}
        {showBlocks
          ? props.groupSection.secondBlockGroups.map((group) => (
              <ParticipantsGroup
                key={group.id}
                groupSection={props.groupSection}
                group={group}
              />
            ))
          : null}
        {showBlocks ? (
          props.groupSection.secondBlockGroups.length > 0 ? (
            <GridLayout
              cols
              colAlign="left"
              height="auto"
              sx={{ px: 2, pt: 1, pb: 2 }}
            >
              <Button
                onClick={() =>
                  navigate(
                    `../${cohortURL(
                      cohort.id,
                      props.groupSection.id,
                      group?.id
                    )}/second/add`
                  )
                }
                variant="contained"
              >
                Add criteria
              </Button>
            </GridLayout>
          ) : (
            <Empty
              maxWidth="90%"
              subtitle={
                <>
                  <Link
                    variant="link"
                    underline="hover"
                    onClick={() =>
                      navigate(
                        `../${cohortURL(
                          cohort.id,
                          props.groupSection.id
                        )}/second/add`
                      )
                    }
                    sx={{ cursor: "pointer" }}
                  >
                    Choose secondary criteria
                  </Link>{" "}
                  to be compared against the main criteria.
                </>
              }
            />
          )
        ) : null}
        {combinedGroups.length !== 0 || cohort.groupSections.length > 1 ? (
          <GridLayout
            cols
            colAlign="right"
            height="auto"
            sx={{
              p: 2,
              boxShadow: (theme) => `inset 0 1px 0 ${theme.palette.divider}`,
            }}
          >
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
    </GridBoxPaper>
  );
}

const filterKindValuePrefixText: { [x: string]: string } = {
  [GroupSectionFilterKind.SameEncounter]: "During same encounter as",
  [GroupSectionFilterKind.DaysWithin]: "On or within",
};

const filterKindValueSuffixText: { [x: string]: string } = {
  [GroupSectionFilterKind.DaysBefore]: "or more days before",
  [GroupSectionFilterKind.DaysAfter]: "or more days after",
  [GroupSectionFilterKind.DaysWithin]: "days of",
};

function ReducingOperator(props: {
  groupSection: GroupSection;
  second?: boolean;
}) {
  const context = useCohortContext();
  const showValue =
    props.second &&
    props.groupSection.filter.kind !== GroupSectionFilterKind.SameEncounter;
  const prefixText = filterKindValuePrefixText[props.groupSection.filter.kind];
  const suffixText = filterKindValueSuffixText[props.groupSection.filter.kind];

  return (
    <GridLayout
      cols
      spacing={1}
      rowAlign="baseline"
      sx={{
        px: 2,
        py: showValue ? 1.2 : 2,
        backgroundColor: (theme) => theme.canvasColor,
      }}
    >
      {props.second ? (
        <GridLayout cols spacing={1} rowAlign="baseline">
          {prefixText ? (
            <Typography variant="body2">{prefixText}</Typography>
          ) : null}
          {showValue ? (
            <TextField
              value={props.groupSection.operatorValue}
              size="small"
              variant="outlined"
              onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                updateCohortGroupSection(context, props.groupSection.id, {
                  operatorValue: parseInt(event.target.value) ?? 0,
                });
              }}
              onClick={(e) => e.stopPropagation()}
              inputProps={{
                type: "number",
                min: 1,
                max: 9999,
              }}
              sx={{
                "& .MuiOutlinedInput-input": (theme) => ({
                  ...theme.typography.body2,
                }),
              }}
            />
          ) : null}
          {suffixText ? (
            <Typography variant="body2">{suffixText}</Typography>
          ) : null}
        </GridLayout>
      ) : null}
      <ReducingOperatorSelect
        groupSection={props.groupSection}
        second={props.second}
      />
      <GridBox />
    </GridLayout>
  );
}

function ReducingOperatorSelect(props: {
  groupSection: GroupSection;
  second?: boolean;
}) {
  const context = useCohortContext();
  return (
    <FormControl>
      <Select
        value={
          props.second
            ? props.groupSection.secondBlockReducingOperator
            : props.groupSection.firstBlockReducingOperator
        }
        onChange={(event: SelectChangeEvent<string>) => {
          updateCohortGroupSection(context, props.groupSection.id, {
            firstBlockReducingOperator: !props.second
              ? (event.target.value as GroupSectionReducingOperator)
              : undefined,
            secondBlockReducingOperator: props.second
              ? (event.target.value as GroupSectionReducingOperator)
              : undefined,
          });
        }}
        sx={{
          color: (theme) => theme.palette.primary.main,
          "& .MuiOutlinedInput-input": {
            px: 0,
            py: "2px",
          },
          "& .MuiSelect-select": (theme) => ({
            ...theme.typography.body2,
          }),
          "& .MuiOutlinedInput-notchedOutline": {
            borderStyle: "none",
          },
        }}
      >
        <MenuItem value={GroupSectionReducingOperator.Any}>
          Any mention of
        </MenuItem>
        <MenuItem value={GroupSectionReducingOperator.First}>
          First mention of
        </MenuItem>
        <MenuItem value={GroupSectionReducingOperator.Last}>
          Last mention of
        </MenuItem>
      </Select>
    </FormControl>
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
  const backendCohort = useBackendCohort();
  const navigate = useNavigate();
  const context = useCohortContext();
  const { groupId } = useParams<{ groupId: string }>();

  const backendGroupSection = useMemo(
    () =>
      backendCohort.groupSections.find((gs) => gs.id === props.groupSection.id),
    [backendCohort, props.groupSection]
  );

  const backendGroup = useMemo(
    () =>
      backendGroupSection?.groups?.find((g) => g.id === props.group.id) ??
      backendGroupSection?.secondBlockGroups?.find(
        (g) => g.id === props.group.id
      ),
    [backendCohort, backendGroupSection, props.group]
  );

  const fetchGroupCount = useCallback(async () => {
    if (!backendGroup || !backendGroupSection) {
      return undefined;
    }

    return (
      await studySource.cohortCount(
        studyId,
        cohort.id,
        backendGroupSection.id,
        backendGroup.id
      )
    )[0].count;
  }, [
    studyId,
    cohort.id,
    cohort.underlayName,
    backendGroupSection,
    backendGroup,
  ]);

  const groupCountState = useSWRImmutable(
    {
      component: "Overview",
      studyId,
      cohortId: cohort.id,
      underlayName: cohort.underlayName,
      group: backendGroup,
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
        const p = getCriteriaPlugin(
          c,
          plugin.filterEntityIds?.(underlaySource)?.[0] ?? ""
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
    <GridLayout key={props.group.id} rows height="auto">
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
          <GridLayout cols fillCol={3} height="auto" rowAlign="middle">
            {isTemporalSection(props.groupSection) &&
            !selector.supportsTemporalQueries ? (
              <Tooltip
                title={`${title} is ineligible for temporal comparison`}
                arrow={true}
              >
                <ErrorOutlineIcon color="error" sx={{ display: "block" }} />
              </Tooltip>
            ) : (
              <GridBox />
            )}
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
            {selected ? (
              <GridBox
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                }}
                onMouseUp={(e) => {
                  e.stopPropagation();
                }}
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
            ) : (
              <GridBox />
            )}
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
                {isValid(groupCountState.data)
                  ? groupCountState.data?.toLocaleString()
                  : ""}
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
                        {isTemporalSection(props.groupSection) &&
                        !modifierCriteria[i].config.supportsTemporalQueries ? (
                          <Tooltip
                            title={`${modifierCriteria[i].config.displayName} is ineligible for temporal comparison`}
                            arrow={true}
                          >
                            <ErrorOutlineIcon
                              color="error"
                              sx={{ display: "block" }}
                            />
                          </Tooltip>
                        ) : null}
                        <Typography variant="body2">
                          {modifierCriteria[i].config.displayName}
                        </Typography>
                        <GridBox
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                          }}
                          onMouseUp={(e) => {
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
                  <GridLayout cols rowAlign="middle" sx={{ pl: 4 }}>
                    {isTemporalSection(props.groupSection) &&
                    !modifierCriteria[i].config.supportsTemporalQueries ? (
                      <Tooltip
                        title={`${t} is ineligible for temporal comparison`}
                        arrow={true}
                      >
                        <ErrorOutlineIcon
                          color="error"
                          sx={{ fontSize: 16, display: "block" }}
                        />
                      </Tooltip>
                    ) : null}
                    <Typography variant="body2">{t}</Typography>
                  </GridLayout>
                )
              )}
            </GridLayout>
          ) : null}
        </GridLayout>
        {menu}
      </GridBox>
      <Divider variant="middle">
        <Chip
          label={
            props.groupSection.filter.kind === GroupSectionFilterKind.All
              ? "AND"
              : "OR"
          }
        />
      </Divider>
    </GridLayout>
  );
}
