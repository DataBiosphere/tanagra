import AddIcon from "@mui/icons-material/Add";
import InfoIcon from "@mui/icons-material/Info";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Link from "@mui/material/Link";
import MenuItem from "@mui/material/MenuItem";
import OutlinedInput from "@mui/material/OutlinedInput";
import Paper from "@mui/material/Paper";
import Select, { SelectChangeEvent } from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Step from "@mui/material/Step";
import StepConnector from "@mui/material/StepConnector";
import StepLabel from "@mui/material/StepLabel";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  generateCohortFilter,
  getCriteriaPlugin,
  getCriteriaTitle,
} from "cohort";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import Loading from "components/loading";
import { TreeGrid, TreeGridData } from "components/treegrid";
import { findEntity } from "data/configuration";
import { Filter, makeArrayFilter } from "data/filter";
import { ExportModel, useSource } from "data/source";
import { useStudyId, useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import React, {
  Fragment,
  ReactNode,
  SyntheticEvent,
  useMemo,
  useState,
} from "react";
import { useNavigate } from "react-router-dom";
import {
  absoluteCohortURL,
  absoluteConceptSetURL,
  absoluteNewConceptSetURL,
  useBaseParams,
} from "router";
import { StudyName } from "studyName";
import useSWR from "swr";
import useSWRImmutable from "swr/immutable";
import * as tanagra from "tanagra-api";
import { useImmer } from "use-immer";
import { isValid } from "util/valid";

export function Datasets() {
  const source = useSource();
  const studyId = useStudyId();
  const unfilteredCohorts = useSWR(
    { type: "cohort", studyId, list: true },
    async () => await source.listCohorts(studyId)
  );

  const workspaceConceptSets = useSWR(
    { type: "conceptSet", studyId, list: true },
    async () =>
      await source
        .listConceptSets(studyId)
        .then((conceptSets) =>
          conceptSets.filter((cs) => cs.underlayName === underlay.name)
        )
  );

  const navigate = useNavigate();

  const underlay = useUnderlay();
  const params = useBaseParams();

  const [selectedCohorts, updateSelectedCohorts] = useImmer(new Set<string>());
  const [selectedConceptSets, updateSelectedConceptSets] = useImmer(
    new Set<string>()
  );
  const [excludedAttributes, updateExcludedAttributes] = useImmer(
    new Map<string, Set<string>>()
  );

  const conceptSetOccurrences = useConceptSetOccurrences(
    selectedConceptSets,
    workspaceConceptSets.data
  );

  const newCohort = async () => {
    const cohort = await source.createCohort(
      underlay.name,
      studyId,
      `Untitled cohort ${new Date().toLocaleString()}`
    );
    navigate(absoluteCohortURL(params, cohort.id, cohort.groupSections[0].id));
  };

  const onToggle = <T,>(
    update: (fn: (draft: Set<T>) => Set<T>) => void,
    id: T
  ) => {
    update((draft) => {
      if (draft.has(id)) {
        draft.delete(id);
      } else {
        draft.add(id);
      }
      return draft;
    });
  };

  const listConceptSets = (
    editable: boolean,
    conceptSets: { id: string; name: string }[]
  ) => {
    // TODO(tjennison): Only show demographics until we have better prepackaged
    // concept sets.
    const cs = conceptSets;
    if (!editable) {
      cs.splice(1, Infinity);
    }

    return cs.map((conceptSet, i) => (
      <GridLayout
        key={conceptSet.id}
        cols
        fillCol={2}
        rowAlign="middle"
        height="auto"
        sx={{
          boxShadow:
            i !== 0 || editable
              ? (theme) => `0 -1px 0 ${theme.palette.divider}`
              : undefined,
        }}
      >
        <Checkbox
          name={conceptSet.name}
          checked={selectedConceptSets.has(conceptSet.id)}
          onChange={() => onToggle(updateSelectedConceptSets, conceptSet.id)}
        />
        <Typography variant="body2" sx={{ my: 0.5 }}>
          {conceptSet.name}
        </Typography>
        <GridBox />
        {editable ? (
          <Button
            data-testid={conceptSet.name}
            variant="outlined"
            onClick={() =>
              navigate(absoluteConceptSetURL(params, conceptSet.id))
            }
            sx={{ minWidth: "auto" }}
          >
            Edit
          </Button>
        ) : null}
      </GridLayout>
    ));
  };

  const cohorts = useMemo(
    () =>
      (unfilteredCohorts.data ?? []).filter(
        (cohort) => cohort.underlayName === underlay.name
      ),
    [unfilteredCohorts.data]
  );

  return (
    <GridLayout rows>
      <ActionBar
        title="Creating dataset"
        subtitle={
          <GridLayout cols spacing={1} rowAlign="baseline">
            <StudyName />
            <Typography variant="body1">•</Typography>
            <Typography variant="body1">
              Data source: {underlay.name}
            </Typography>
          </GridLayout>
        }
        backURL={"/underlays/" + underlay.name}
      />
      <GridBox sx={{ overflowY: "auto" }}>
        <GridLayout height="auto" sx={{ py: 1, px: 5 }}>
          <GridLayout cols="1fr 2fr" spacing={2}>
            <GridLayout rows="auto 320px">
              <GridLayout cols fillCol={2} spacing={1} rowAlign="middle">
                <GridBox sx={{ py: 2 }}>
                  <Typography variant="body1em">Cohorts</Typography>
                </GridBox>
                <Tooltip title="Cohorts are groups of people with common traits">
                  <InfoIcon sx={{ display: "flex" }} />
                </Tooltip>
                <GridBox />
                <Button
                  startIcon={<AddIcon />}
                  variant="contained"
                  onClick={() => newCohort()}
                >
                  New cohort
                </Button>
              </GridLayout>
              <Paper
                sx={{
                  py: 2,
                  overflowY: "auto",
                  display: "block",
                  width: "100%",
                  height: "100%",
                }}
              >
                <GridBox sx={{ px: 1, overflowY: "auto" }}>
                  {cohorts.length === 0 ? (
                    <Empty
                      maxWidth="80%"
                      title="Cohorts are groups of people with common traits"
                      subtitle={
                        <>
                          <Link
                            variant="link"
                            underline="hover"
                            onClick={() => newCohort()}
                            sx={{ cursor: "pointer" }}
                          >
                            Create a new cohort
                          </Link>{" "}
                          to define criteria
                        </>
                      }
                    />
                  ) : (
                    cohorts
                      .filter((cohort) => cohort.underlayName === underlay.name)
                      .map((cohort, i) => (
                        <GridLayout
                          key={cohort.id}
                          cols
                          fillCol={2}
                          rowAlign="middle"
                          height="auto"
                          sx={{
                            boxShadow:
                              i !== 0
                                ? (theme) => `0 -1px 0 ${theme.palette.divider}`
                                : undefined,
                          }}
                        >
                          <Checkbox
                            name={cohort.name}
                            checked={selectedCohorts.has(cohort.id)}
                            onChange={() =>
                              onToggle(updateSelectedCohorts, cohort.id)
                            }
                          />
                          <Typography variant="body2" sx={{ my: 0.5 }}>
                            {cohort.name}
                          </Typography>
                          <GridBox />
                          <Button
                            data-testid={cohort.name}
                            variant="outlined"
                            onClick={() =>
                              navigate(
                                absoluteCohortURL(
                                  params,
                                  cohort.id,
                                  cohort.groupSections[0].id
                                )
                              )
                            }
                            sx={{ minWidth: "auto" }}
                          >
                            Edit
                          </Button>
                        </GridLayout>
                      ))
                  )}
                </GridBox>
              </Paper>
            </GridLayout>
            <GridLayout rows="auto 320px">
              <GridLayout cols fillCol={2} spacing={1} rowAlign="middle">
                <GridBox sx={{ py: 2 }}>
                  <Typography variant="body1em" sx={{ py: 2 }}>
                    Data features
                  </Typography>
                </GridBox>
                <Tooltip title="Data features are categories of data to export">
                  <InfoIcon sx={{ display: "flex" }} />
                </Tooltip>
                <GridBox />
                <Button
                  startIcon={<AddIcon />}
                  variant="contained"
                  onClick={() => {
                    navigate(absoluteNewConceptSetURL(params));
                  }}
                >
                  New data feature
                </Button>
              </GridLayout>
              <Paper
                sx={{
                  p: 2,
                  display: "block",
                  width: "100%",
                  height: "100%",
                }}
              >
                <GridLayout cols="1fr 1fr" spacing={2}>
                  <GridLayout rows spacing={2}>
                    <GridLayout cols>
                      <Step
                        index={0}
                        active={conceptSetOccurrences.length === 0}
                        completed={conceptSetOccurrences.length > 0}
                      >
                        <StepLabel>Select data features</StepLabel>
                      </Step>
                      <StepConnector />
                    </GridLayout>
                    <Paper
                      variant="outlined"
                      sx={{
                        py: 2,
                        display: "block",
                        width: "100%",
                        height: "100%",
                      }}
                    >
                      <GridBox sx={{ px: 1, overflowY: "auto" }}>
                        {underlay.uiConfiguration.prepackagedConceptSets ? (
                          <>
                            {listConceptSets(
                              false,
                              underlay.uiConfiguration.prepackagedConceptSets
                            )}
                          </>
                        ) : null}
                        {workspaceConceptSets.data?.length
                          ? listConceptSets(
                              true,
                              (workspaceConceptSets.data ?? []).map((cs) => ({
                                id: cs.id,
                                name: getCriteriaTitle(cs.criteria),
                              }))
                            )
                          : null}
                      </GridBox>
                    </Paper>
                  </GridLayout>
                  <GridLayout rows spacing={2}>
                    <Step index={1} active={conceptSetOccurrences.length > 0}>
                      <StepLabel>Select feature values</StepLabel>
                    </Step>
                    <Paper
                      variant="outlined"
                      sx={{
                        py: 2,
                        display: "block",
                        width: "100%",
                        height: "100%",
                      }}
                    >
                      <GridBox sx={{ px: 1, overflowY: "auto" }}>
                        {conceptSetOccurrences.length === 0 && (
                          <Empty
                            maxWidth="80%"
                            title="No data features selected"
                            subtitle="Select at least one data feature to pick values"
                          />
                        )}
                        {conceptSetOccurrences.map((occurrence) => (
                          <Fragment key={occurrence.id}>
                            <GridBox
                              sx={{
                                position: "sticky",
                                top: 0,
                                zIndex: 1,
                                backgroundColor: (theme) =>
                                  theme.palette.background.paper,
                                boxShadow: (theme) =>
                                  `inset 0 -1px 0 ${theme.palette.divider}`,
                                height: "auto",
                              }}
                            >
                              <Typography variant="body2em">
                                {occurrence.name}
                              </Typography>
                            </GridBox>
                            {occurrence.attributes.map((attribute) => (
                              <Stack
                                key={attribute}
                                direction="row"
                                alignItems="center"
                              >
                                <Checkbox
                                  name={occurrence.id + "-" + attribute}
                                  checked={
                                    !excludedAttributes
                                      .get(occurrence.id)
                                      ?.has(attribute)
                                  }
                                  onChange={() =>
                                    updateExcludedAttributes((selection) => {
                                      if (!selection?.get(occurrence.id)) {
                                        selection?.set(
                                          occurrence.id,
                                          new Set<string>()
                                        );
                                      }

                                      const attributes = selection?.get(
                                        occurrence.id
                                      );
                                      if (attributes?.has(attribute)) {
                                        attributes?.delete(attribute);
                                      } else {
                                        attributes?.add(attribute);
                                      }
                                    })
                                  }
                                />
                                <Typography variant="body2">
                                  {attribute}
                                </Typography>
                              </Stack>
                            ))}
                          </Fragment>
                        ))}
                      </GridBox>
                    </Paper>
                  </GridLayout>
                </GridLayout>
              </Paper>
            </GridLayout>
          </GridLayout>
          <Preview
            selectedCohorts={selectedCohorts}
            selectedConceptSets={selectedConceptSets}
            conceptSetOccurrences={conceptSetOccurrences}
            excludedAttributes={excludedAttributes}
          />
        </GridLayout>
      </GridBox>
    </GridLayout>
  );
}

type ConceptSetOccurrence = {
  id: string;
  name: string;
  attributes: string[];
  filters: Filter[];
};

function useConceptSetOccurrences(
  selectedConceptSets: Set<string>,
  workspaceConceptSets?: tanagra.ConceptSet[]
): ConceptSetOccurrence[] {
  const underlay = useUnderlay();
  const source = useSource();

  return useMemo(() => {
    const occurrences = new Map<string, Filter[]>();
    const addFilter = (occurrence: string, filter?: Filter | null) => {
      if (!occurrences.has(occurrence)) {
        occurrences.set(occurrence, []);
      }
      if (filter) {
        occurrences.get(occurrence)?.push(filter);
      }
    };

    underlay.uiConfiguration.prepackagedConceptSets?.forEach((conceptSet) => {
      if (selectedConceptSets.has(conceptSet.id)) {
        addFilter(conceptSet.occurrence, conceptSet.filter);
      }
    });

    workspaceConceptSets
      ?.filter((cs) => selectedConceptSets.has(cs.id))
      ?.forEach((conceptSet) => {
        const plugin = getCriteriaPlugin(conceptSet.criteria);
        const occurrenceIds = plugin.outputOccurrenceIds?.() ?? [
          plugin.filterOccurrenceId(),
        ];
        occurrenceIds.forEach((o) => {
          addFilter(o, plugin.generateFilter());
        });
      });

    return Array.from(occurrences)
      .sort()
      .map(([id, filters]) => {
        return {
          id,
          name: findEntity(id, source.config).entity,
          attributes: source.listAttributes(id),
          filters,
        };
      });
  }, [selectedConceptSets, workspaceConceptSets]);
}

type PreviewProps = {
  selectedCohorts: Set<string>;
  selectedConceptSets: Set<string>;
  conceptSetOccurrences: ConceptSetOccurrence[];
  excludedAttributes: Map<string, Set<string>>;
};

function Preview(props: PreviewProps) {
  const source = useSource();
  const studyId = useStudyId();

  const unfilteredCohorts = useSWR(
    { type: "cohort", studyId, list: true },
    async () => await source.listCohorts(studyId)
  );

  const cohorts = useMemo(
    () =>
      (unfilteredCohorts.data ?? []).filter((cohort) =>
        props.selectedCohorts.has(cohort.id)
      ),
    [unfilteredCohorts.data, props.selectedCohorts]
  );

  const cohortsFilter = useMemo(
    () =>
      makeArrayFilter(
        { min: 1 },
        (cohorts || []).map((cohort) => generateCohortFilter(cohort))
      ),
    [cohorts]
  );

  const conceptSetParams = useMemo(
    () =>
      props.conceptSetOccurrences.map((occurrence) => ({
        id: occurrence.id,
        name: occurrence.name,
        filter: makeArrayFilter({ min: 1 }, occurrence.filters),
        attributes: occurrence.attributes.filter(
          (a) => !props.excludedAttributes.get(occurrence.id)?.has(a)
        ),
      })),
    [props.conceptSetOccurrences, props.excludedAttributes]
  );

  const [tab, setTab] = useState(0);
  const [queriesMode, setQueriesMode] = useState<boolean | null>(false);

  const tabDataState = useSWRImmutable<PreviewTabData[]>(
    {
      type: "previewData",
      cohorts,
      occurrences: props.conceptSetOccurrences,
      excludedAtrtibutes: props.excludedAttributes,
    },
    async () => {
      return Promise.all(
        conceptSetParams.map(async (params) => {
          if (!cohortsFilter) {
            throw new Error("No selected cohort contain any criteria.");
          }

          const res = await source.listData(
            params.attributes,
            params.id,
            cohortsFilter,
            params.filter
          );

          const data: TreeGridData = {
            root: { data: {}, children: [] },
          };

          res.data.forEach((entry, i) => {
            data[i] = { data: entry };
            data.root?.children?.push(i);
          });

          return {
            name: params.name,
            sql: res.sql,
            data: data,
          };
        })
      );
    }
  );

  const [exportDialog, showExportDialog] = useExportDialog({
    cohorts: cohorts,
    cohortsFilter: cohortsFilter,
    conceptSetParams: conceptSetParams,
  });

  const onTabChange = (event: SyntheticEvent, newValue: number) => {
    setTab(newValue);
  };

  const onQueriesModeChange = (
    event: React.MouseEvent<HTMLElement>,
    value: boolean | null
  ) => {
    if (isValid(value)) {
      setQueriesMode(value);
    }
  };

  const empty =
    props.selectedCohorts.size === 0 || props.selectedConceptSets.size === 0;

  return (
    <GridLayout rows height="auto">
      <GridLayout cols fillCol={1} rowAlign="middle">
        <GridBox sx={{ py: 2 }}>
          <Typography variant="body1em">Dataset preview</Typography>
        </GridBox>
        <GridBox />
        <Button
          variant="contained"
          disabled={empty}
          onClick={() => {
            showExportDialog();
          }}
        >
          Export
        </Button>
        {exportDialog}
      </GridLayout>
      <Paper sx={{ p: 1 }}>
        {!empty ? (
          <Loading status={tabDataState}>
            <Stack
              direction="row"
              alignItems="center"
              sx={{ borderBottom: 1, borderColor: "divider" }}
            >
              <Tabs value={tab} onChange={onTabChange} sx={{ flexGrow: 1 }}>
                {tabDataState.data?.map((data) => (
                  <Tab key={data.name} label={data.name} />
                ))}
              </Tabs>
              <ToggleButtonGroup
                value={queriesMode}
                exclusive
                onChange={onQueriesModeChange}
              >
                <ToggleButton value={false}>Data</ToggleButton>
                <ToggleButton value={true}>Queries</ToggleButton>
              </ToggleButtonGroup>
            </Stack>
            {queriesMode ? (
              <Typography sx={{ fontFamily: "monospace" }}>
                {tabDataState.data?.[tab]?.sql}
              </Typography>
            ) : tabDataState.data?.[tab]?.data ? (
              <div
                style={{
                  overflowX: "auto",
                  display: "block",
                }}
              >
                {tabDataState.data?.[tab]?.data?.root?.children?.length ? (
                  <TreeGrid
                    data={tabDataState.data?.[tab]?.data}
                    columns={props.conceptSetOccurrences[tab]?.attributes
                      .filter(
                        (a) =>
                          !props.excludedAttributes
                            .get(props.conceptSetOccurrences[tab]?.id)
                            ?.has(a)
                      )
                      .map((attribute) => ({
                        key: attribute,
                        width: 140,
                        title: attribute,
                      }))}
                    minWidth
                    wrapBodyText
                    rowHeight="auto"
                    padding={0}
                  />
                ) : (
                  <Empty
                    maxWidth="60%"
                    minHeight="200px"
                    image="/empty.svg"
                    title="No data matched"
                    subtitle="No data in this table matched the specified cohorts and data features"
                  />
                )}
              </div>
            ) : undefined}
          </Loading>
        ) : (
          <Empty
            maxWidth="60%"
            minHeight="200px"
            image="/empty.svg"
            subtitle="Select at least one cohort & data feature to preview your dataset"
          />
        )}
      </Paper>
    </GridLayout>
  );
}

type PreviewTabData = {
  name: string;
  sql: string;
  data: TreeGridData;
};

type ConceptSetParams = {
  id: string;
  name: string;
  filter: Filter | null;
  attributes: string[];
};

type ExportDialogProps = {
  cohorts: tanagra.Cohort[];
  cohortsFilter: Filter | null;
  conceptSetParams: ConceptSetParams[];
};

function useExportDialog(props: ExportDialogProps): [ReactNode, () => void] {
  const [open, setOpen] = useState(false);
  const show = () => setOpen(true);

  return [
    // eslint-disable-next-line react/jsx-key
    <ExportDialog {...props} open={open} hide={() => setOpen(false)} />,
    show,
  ];
}

function ExportDialog(
  props: ExportDialogProps & { open: boolean; hide: () => void }
) {
  const source = useSource();
  const underlay = useUnderlay();
  const studyId = useStudyId();

  const [selId, setSelId] = useState<string | undefined>(undefined);
  const [exporting, setExporting] = useState(false);
  const [instance, setInstance] = useState(0);
  const [output, setOutput] = useState<ReactNode>(null);

  const exportModelsState = useSWRImmutable<ExportModel[]>(
    {
      type: "exportModel",
      underlayName: underlay.name,
    },
    async () => {
      return await source.listExportModels(underlay.name);
    }
  );

  const onSelectModel = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    setSelId(sel);
  };

  const model =
    exportModelsState.data?.find((m) => m.id === selId) ??
    exportModelsState.data?.[0];

  const onExport = async (startInstance: number) => {
    if (!model) {
      throw new Error("No export method selected.");
    }

    const cohortsFilter = props.cohortsFilter;
    if (!cohortsFilter) {
      throw new Error("All selected cohorts are empty.");
    }

    setExporting(true);
    setOutput(null);

    const result = await source.export(
      underlay.name,
      studyId,
      model.id,
      window.location.href,
      props.cohorts.map((c) => c.id),
      props.conceptSetParams.map((params) => ({
        requestedAttributes: params.attributes,
        occurrenceID: params.id,
        cohort: cohortsFilter,
        conceptSet: params.filter,
      }))
    );

    if (instance === startInstance) {
      if (result.redirectURL) {
        setOutput(<CenteredContent progress text="Redirecting..." />);
        setExporting(false);

        window.location.href = result.redirectURL;
      } else {
        // TODO(tjennison): Use plugins to handle different types of output.
        const annotations: { cohortId: string; url: string }[] = [];
        const data: { occurrenceId: string; url: string }[] = [];

        for (const key in result.outputs) {
          const parts = key.split(":");
          if (parts.length !== 2) {
            throw new Error(`Invalid output key ${key}.`);
          }

          if (parts[0] === "cohort") {
            annotations.push({ cohortId: parts[1], url: result.outputs[key] });
          } else if (parts[0] === "entity") {
            data.push({ occurrenceId: parts[1], url: result.outputs[key] });
          } else {
            throw new Error(`Unknown output type ${parts[0]}`);
          }
        }

        setOutput(
          <GridLayout rows spacing={1} height="auto">
            <Typography variant="body1em">Data</Typography>
            <GridLayout rows height="auto">
              {data.map((d) => (
                <Link href={d.url} variant="body1" key={d.occurrenceId}>
                  {d.occurrenceId}
                </Link>
              ))}
            </GridLayout>
            <Typography variant="body1em">Annotations</Typography>
            <GridLayout rows height="auto">
              {annotations.map((a) => (
                <Link href={a.url} variant="body1" key={a.cohortId}>
                  {props.cohorts.find((c) => c.id === a.cohortId)?.name ??
                    "Unknown"}
                </Link>
              ))}
            </GridLayout>
          </GridLayout>
        );
        setExporting(false);
      }
    }
  };

  return (
    <Dialog
      fullWidth
      maxWidth="sm"
      aria-labelledby="export-dialog-title"
      open={props.open}
      onClose={(event: object, reason: string) => {
        if (reason !== "backdropClick") {
          props.hide();
        }
      }}
    >
      <DialogTitle id="export-dialog-title">Export method</DialogTitle>
      <DialogContent>
        <Loading status={exportModelsState}>
          <GridLayout rows spacing={2} height="auto" sx={{ pt: "2px" }}>
            <Select
              value={model?.id}
              input={<OutlinedInput />}
              disabled={exporting}
              onChange={onSelectModel}
            >
              {exportModelsState.data?.map((m) => (
                <MenuItem key={m.id} value={m.id}>
                  {m.displayName}
                </MenuItem>
              ))}
            </Select>
            {model ? (
              <GridLayout rows spacing={2}>
                <Typography variant="body2em">Description</Typography>
                <Typography variant="body2">{model.description}</Typography>
                <GridLayout colAlign="right" height="auto">
                  <Button
                    variant="contained"
                    disabled={exporting}
                    onClick={() => {
                      setInstance(instance + 1);
                      onExport(instance);
                    }}
                  >
                    Export
                  </Button>
                </GridLayout>
                <GridBox sx={{ minHeight: 200 }}>
                  {exporting ? (
                    <CenteredContent progress text="Exporting..." />
                  ) : (
                    output ?? (
                      <CenteredContent text="Choose an export method and click 'Export'" />
                    )
                  )}
                </GridBox>
              </GridLayout>
            ) : null}
          </GridLayout>
        </Loading>
      </DialogContent>
      <DialogActions>
        <Button
          onClick={() => {
            props.hide();
            setExporting(false);
          }}
        >
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
}

type CenteredContentProps = {
  text: string;
  progress?: boolean;
};

function CenteredContent(props: CenteredContentProps) {
  return (
    <GridLayout rowAlign="middle" colAlign="center" sx={{ minHeight: 200 }}>
      <GridLayout rows spacing={2} colAlign="center" height="auto">
        {props.progress ? <CircularProgress /> : null}
        <Typography variant="body1">{props.text}</Typography>
      </GridLayout>
    </GridLayout>
  );
}
