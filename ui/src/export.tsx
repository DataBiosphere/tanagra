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
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import {
  generateCohortFilter,
  getOccurrenceList,
  OccurrenceFilters,
} from "cohort";
import { CohortSummary } from "cohortSummary";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import Loading from "components/loading";
import { Tabs as TanagraTabs } from "components/tabs";
import { TreeGrid, TreeGridData } from "components/treegrid";
import { Filter, makeArrayFilter } from "data/filter";
import { ExportModel, FeatureSet } from "data/source";
import { useSource } from "data/sourceContext";
import { useStudyId, useUnderlay } from "hooks";
import emptyImage from "images/empty.svg";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { OccurrenceFiltersSummary } from "occurrenceFiltersSummary";
import React, { ReactNode, SyntheticEvent, useMemo, useState } from "react";
import {
  absoluteExportURL,
  cohortURL,
  featureSetURL,
  redirect,
  RETURN_URL_PLACEHOLDER,
  useBaseParams,
  useExitAction,
} from "router";
import { StudyName } from "studyName";
import useSWR from "swr";
import useSWRImmutable from "swr/immutable";
import * as tanagraUI from "tanagra-ui";
import { useImmer } from "use-immer";
import { useNavigate } from "util/searchState";
import { isValid } from "util/valid";

export function Export() {
  const source = useSource();
  const studyId = useStudyId();
  const exit = useExitAction();
  const navigate = useNavigate();
  const underlay = useUnderlay();

  const cohortsState = useSWR(
    { type: "cohort", studyId, list: true },
    async () => await source.listCohorts(studyId)
  );

  const cohorts = useMemo(
    () =>
      (cohortsState.data ?? []).filter(
        (cohort) => cohort.underlayName === underlay.name
      ),
    [cohortsState.data]
  );

  const featureSetsState = useSWR(
    { type: "featureSet", studyId, list: true },
    async () => await source.listFeatureSets(studyId)
  );

  const featureSets = useMemo(
    () =>
      (featureSetsState.data ?? []).filter(
        (fs) => fs.underlayName === underlay.name
      ),
    [featureSetsState.data]
  );

  const [selectedCohorts, updateSelectedCohorts] = useImmer(new Set<string>());
  const [selectedFeatureSets, updateSelectedFeatureSets] = useImmer(
    new Set<string>()
  );

  const newCohort = async () => {
    const cohort = await source.createCohort(
      underlay.name,
      studyId,
      `Untitled cohort ${new Date().toLocaleString()}`
    );
    navigate("../" + cohortURL(cohort.id, cohort.groupSections[0].id));
  };

  const newFeatureSet = async () => {
    const featureSet = await source.createFeatureSet(
      underlay.name,
      studyId,
      `Untitled feature set ${new Date().toLocaleString()}`
    );
    navigate("../" + featureSetURL(featureSet.id));
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

  return (
    <GridLayout rows>
      <ActionBar
        title="Exporting dataset"
        subtitle={
          <GridLayout cols spacing={1} rowAlign="baseline">
            <StudyName />
            <Typography variant="body1">•</Typography>
            <Typography variant="body1">
              Data source: {underlay.name}
            </Typography>
          </GridLayout>
        }
        backAction={exit}
      />
      <GridBox>
        <GridLayout rows="2fr 3fr" sx={{ pb: 2, px: 5 }}>
          <GridLayout cols="1fr 1fr" spacing={2}>
            <GridLayout rows>
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
                    cohorts.map((cohort, i) => (
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
                              "../" +
                                cohortURL(cohort.id, cohort.groupSections[0].id)
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
            <GridLayout rows>
              <GridLayout cols fillCol={2} spacing={1} rowAlign="middle">
                <GridBox sx={{ py: 2 }}>
                  <Typography variant="body1em" sx={{ py: 2 }}>
                    Data feature sets
                  </Typography>
                </GridBox>
                <Tooltip title="Data feature sets are configurations of data to export">
                  <InfoIcon sx={{ display: "flex" }} />
                </Tooltip>
                <GridBox />
                <Button
                  startIcon={<AddIcon />}
                  variant="contained"
                  onClick={() => newFeatureSet()}
                >
                  New data feature set
                </Button>
              </GridLayout>
              <Paper
                sx={{
                  py: 2,
                  width: "100%",
                  height: "100%",
                }}
              >
                <GridBox sx={{ px: 1, overflowY: "auto" }}>
                  {featureSets.length === 0 ? (
                    <Empty
                      maxWidth="80%"
                      title="Data features decide the data shown for each participant"
                      subtitle={
                        <>
                          <Link
                            variant="link"
                            underline="hover"
                            onClick={() => newFeatureSet()}
                            sx={{ cursor: "pointer" }}
                          >
                            Create a data feature set
                          </Link>{" "}
                          to explore domains of interest
                        </>
                      }
                    />
                  ) : (
                    featureSets.map((fs, i) => (
                      <GridLayout
                        key={fs.id}
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
                          name={fs.name}
                          checked={selectedFeatureSets.has(fs.id)}
                          onChange={() =>
                            onToggle(updateSelectedFeatureSets, fs.id)
                          }
                        />
                        <Typography variant="body2" sx={{ my: 0.5 }}>
                          {fs.name}
                        </Typography>
                        <GridBox />
                        <Button
                          data-testid={fs.name}
                          variant="outlined"
                          onClick={() => navigate("../" + featureSetURL(fs.id))}
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
          </GridLayout>
          <Preview
            cohorts={cohorts}
            featureSets={featureSets}
            selectedCohorts={selectedCohorts}
            selectedFeatureSets={selectedFeatureSets}
          />
        </GridLayout>
      </GridBox>
    </GridLayout>
  );
}

type PreviewProps = {
  cohorts: tanagraUI.UICohort[];
  featureSets: FeatureSet[];
  selectedCohorts: Set<string>;
  selectedFeatureSets: Set<string>;
};

function Preview(props: PreviewProps) {
  const underlay = useUnderlay();
  const source = useSource();

  const filteredCohorts = useMemo(
    () =>
      (props.cohorts ?? []).filter((cohort) =>
        props.selectedCohorts.has(cohort.id)
      ),
    [props.cohorts, props.selectedCohorts]
  );

  const cohortsFilter = useMemo(
    () =>
      makeArrayFilter(
        { min: 1 },
        filteredCohorts.map((cohort) => generateCohortFilter(cohort))
      ),
    [filteredCohorts]
  );

  const filteredFeatureSets = useMemo(
    () =>
      props.featureSets.filter((fs) => props.selectedFeatureSets.has(fs.id)),
    [props.featureSets, props.selectedFeatureSets]
  );

  const occurrenceFilters = useMemo(() => {
    const occurrenceLists = filteredFeatureSets.map((fs) => {
      const ol = getOccurrenceList(
        source,
        new Set(fs.criteria.map((c) => c.id).concat(fs.predefinedCriteria)),
        fs.criteria,
        underlay.uiConfiguration.prepackagedConceptSets
      );

      ol.forEach((of) => {
        const output = fs.output.find((o) => o.occurrence === of.id);
        if (!output) {
          return;
        }

        of.attributes = of.attributes.filter(
          (a) => output.excludedAttributes.indexOf(a) < 0
        );
      });
      return ol;
    });

    const merged: OccurrenceFilters[] = [];
    occurrenceLists.forEach((ol) => {
      ol.forEach((of) => {
        const cur = merged.find((f) => f.id === of.id);
        if (!cur) {
          merged.push(of);
          return;
        }

        cur.attributes = Array.from(
          new Set(cur.attributes.concat(of.attributes))
        );
        cur.filters = cur.filters.concat(of.filters);
        cur.sourceCriteria = cur.sourceCriteria.concat(of.sourceCriteria);
      });
    });

    return merged;
  }, [filteredFeatureSets]);

  const [exportDialog, showExportDialog] = useExportDialog({
    cohorts: props.cohorts,
    cohortsFilter: cohortsFilter,
    occurrenceFilters: occurrenceFilters,
  });

  const empty =
    props.selectedCohorts.size === 0 || props.selectedFeatureSets.size === 0;

  return (
    <GridBox>
      <TanagraTabs
        configs={[
          {
            id: "summary",
            title: "Summary",
            render: () => (
              <PreviewSummary
                cohorts={filteredCohorts}
                occurrenceFilters={occurrenceFilters}
                empty={empty}
              />
            ),
          },
          {
            id: "tables",
            title: "Data",
            render: () => (
              <PreviewTable
                cohortsFilter={cohortsFilter}
                occurrenceFilters={occurrenceFilters}
                empty={empty}
              />
            ),
          },
        ]}
        center
        hideDivider
        tabsPrefix={
          <GridBox sx={{ width: 200 }}>
            <Typography variant="body1em">Export preview</Typography>
          </GridBox>
        }
        tabsSuffix={
          <GridLayout cols colAlign="right" sx={{ width: 200 }}>
            <Button
              variant="contained"
              disabled={empty}
              onClick={() => {
                showExportDialog();
              }}
            >
              Export dataset
            </Button>
          </GridLayout>
        }
      />
      {exportDialog}
    </GridBox>
  );
}

type PreviewTabData = {
  name: string;
  sql: string;
  data: TreeGridData;
};

type PreviewTableProps = {
  cohortsFilter: Filter | null;
  occurrenceFilters: OccurrenceFilters[];
  empty: boolean;
};

function PreviewTable(props: PreviewTableProps) {
  const source = useSource();

  const [tab, setTab] = useState(0);
  const [queriesMode, setQueriesMode] = useState<boolean | null>(false);

  const tabDataState = useSWRImmutable<PreviewTabData[]>(
    {
      type: "previewData",
      cohortsFilter: props.cohortsFilter,
      occurrences: props.occurrenceFilters,
    },
    async () => {
      return Promise.all(
        props.occurrenceFilters.map(async (filters) => {
          if (!props.cohortsFilter) {
            throw new Error("No selected cohort contain any criteria.");
          }

          const res = await source.listData(
            filters.attributes,
            filters.id,
            props.cohortsFilter,
            makeArrayFilter({ min: 1 }, filters.filters)
          );

          const data: TreeGridData = {
            root: { data: {}, children: [] },
          };

          res.data.forEach((entry, i) => {
            data[i] = { data: entry };
            data.root?.children?.push(i);
          });

          return {
            name: filters.name,
            sql: res.sql,
            data: data,
          };
        })
      );
    }
  );

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

  return (
    <Paper sx={{ p: 1, width: "100%", height: "100%" }}>
      {!props.empty ? (
        <Loading status={tabDataState}>
          <GridLayout cols>
            <GridLayout
              rows
              colAlign="center"
              sx={{ borderRight: 1, borderColor: "divider" }}
            >
              <ToggleButtonGroup
                value={queriesMode}
                exclusive
                onChange={onQueriesModeChange}
              >
                <ToggleButton value={false}>Tables</ToggleButton>
                <ToggleButton value={true}>Queries</ToggleButton>
              </ToggleButtonGroup>
              <Tabs
                orientation="vertical"
                value={tab}
                onChange={onTabChange}
                sx={{ flexGrow: 1 }}
              >
                {tabDataState.data?.map((data) => (
                  <Tab key={data.name} label={data.name} />
                ))}
              </Tabs>
            </GridLayout>
            <GridBox>
              {queriesMode ? (
                <Typography sx={{ whiteSpace: "pre", fontFamily: "monospace" }}>
                  {tabDataState.data?.[tab]?.sql}
                </Typography>
              ) : tabDataState.data?.[tab]?.data ? (
                tabDataState.data?.[tab]?.data?.root?.children?.length ? (
                  <TreeGrid
                    data={tabDataState.data?.[tab]?.data}
                    columns={props.occurrenceFilters[tab]?.attributes.map(
                      (attribute) => ({
                        key: attribute,
                        width: 140,
                        title: attribute,
                      })
                    )}
                    minWidth
                    wrapBodyText
                    rowHeight="auto"
                  />
                ) : (
                  <Empty
                    maxWidth="60%"
                    minHeight="200px"
                    image={emptyImage}
                    title="No data matched"
                    subtitle="No data in this table matched the specified cohorts and data features"
                  />
                )
              ) : undefined}
            </GridBox>
          </GridLayout>
        </Loading>
      ) : (
        <Empty
          maxWidth="60%"
          minHeight="200px"
          image={emptyImage}
          subtitle="Select at least one cohort & data feature to preview your dataset"
        />
      )}
    </Paper>
  );
}

type PreviewSummaryProps = {
  cohorts: tanagraUI.UICohort[];
  occurrenceFilters: OccurrenceFilters[];
  empty: boolean;
};

function PreviewSummary(props: PreviewSummaryProps) {
  return (
    <Paper sx={{ p: 2, width: "100%", height: "100%" }}>
      {!props.empty ? (
        <GridLayout cols="1fr 1fr" spacing={2} sx={{ overflowY: "auto" }}>
          <GridLayout rows spacing={1}>
            <Typography variant="body1em">
              Participants in the tables share the following characteristics
            </Typography>
            {props.cohorts.map((cohort) => (
              <Paper
                key={cohort.id}
                sx={{
                  p: 1,
                  backgroundColor: (theme) => theme.palette.background.default,
                }}
              >
                <CohortSummary cohort={cohort} />
              </Paper>
            ))}
          </GridLayout>
          <GridLayout rows spacing={1}>
            <Typography variant="body1em">
              Here are the {props.occurrenceFilters.length} tables you get in
              your export
            </Typography>
            {props.occurrenceFilters.map((filters) => (
              <Paper
                key={filters.id}
                sx={{
                  p: 1,
                  backgroundColor: (theme) => theme.palette.background.default,
                }}
              >
                <OccurrenceFiltersSummary filters={filters} />
              </Paper>
            ))}
          </GridLayout>
        </GridLayout>
      ) : (
        <Empty
          maxWidth="60%"
          minHeight="200px"
          image={emptyImage}
          subtitle="Select at least one cohort & data feature to preview your dataset"
        />
      )}
    </Paper>
  );
}

type ExportDialogProps = {
  cohorts: tanagraUI.UICohort[];
  cohortsFilter: Filter | null;
  occurrenceFilters: OccurrenceFilters[];
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
  const params = useBaseParams();

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
      RETURN_URL_PLACEHOLDER,
      props.cohorts.map((c) => c.id),
      props.occurrenceFilters.map((filters) => ({
        requestedAttributes: filters.attributes,
        occurrenceID: filters.id,
        cohort: cohortsFilter,
        conceptSet: makeArrayFilter({ min: 1 }, filters.filters),
      }))
    );

    if (instance === startInstance) {
      if (result.redirectURL) {
        setOutput(<CenteredContent progress text="Redirecting..." />);
        setExporting(false);

        redirect(result.redirectURL, absoluteExportURL(params));
      } else {
        const artifactLists: {
          section: string;
          name: string;
          url: string;
        }[][] = [];

        for (const key in result.outputs) {
          const parts = key.split(":");
          if (parts.length > 2) {
            throw new Error(`Invalid output key ${key}.`);
          }

          const artifact = {
            section: parts.length > 1 ? parts[0] : "",
            name: parts.length > 1 ? parts[1] : parts[0],
            url: result.outputs[key],
          };
          const list = artifactLists.find(
            (list) => list[0].section === artifact.section
          );
          if (list) {
            list.push(artifact);
          } else {
            artifactLists.push([artifact]);
          }
        }

        artifactLists.sort((a, b) => a[0].section.localeCompare(b[0].section));

        // TODO(tjennison): Remove fallback for looking up cohort ids once the
        // backend plugin returns them.
        setOutput(
          <GridLayout rows spacing={1} height="auto">
            {artifactLists.map((list) => (
              <GridLayout key={list[0].section} rows spacing={1} height="auto">
                {list[0].section ? (
                  <Typography variant="body1em">{list[0].section}</Typography>
                ) : null}
                <GridLayout rows height="auto">
                  {list.map((artifact) => (
                    <Link
                      href={artifact.url}
                      variant="body1"
                      key={artifact.name}
                    >
                      {props.cohorts.find((c) => c.id === artifact.name)
                        ?.name ?? artifact.name}
                    </Link>
                  ))}
                </GridLayout>
              </GridLayout>
            ))}
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
