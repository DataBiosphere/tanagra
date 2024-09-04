import AddIcon from "@mui/icons-material/Add";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import DownloadIcon from "@mui/icons-material/Download";
import InfoIcon from "@mui/icons-material/Info";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import FormControl from "@mui/material/FormControl";
import Link from "@mui/material/Link";
import Paper from "@mui/material/Paper";
import Radio from "@mui/material/Radio";
import RadioGroup, { useRadioGroup } from "@mui/material/RadioGroup";
import { SelectChangeEvent } from "@mui/material/Select";
import Tab from "@mui/material/Tab";
import Tabs from "@mui/material/Tabs";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { OccurrenceFilters, useOccurrenceList } from "cohort";
import { CohortSummary } from "cohortSummary";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import Loading from "components/loading";
import { Tabs as TanagraTabs } from "components/tabs";
import { TreeGrid, TreeGridData } from "components/treegrid";
import { Cohort, ExportModel, ExportResultLink, FeatureSet } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import { getEnvironment } from "environment";
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
import useSWRMutation from "swr/mutation";
import { useImmer } from "use-immer";
import { useNavigate } from "util/searchState";
import { isValid } from "util/valid";

export function Export() {
  const studySource = useStudySource();
  const underlaySource = useUnderlaySource();
  const studyId = useStudyId();
  const exit = useExitAction();
  const navigate = useNavigate();
  const underlay = useUnderlay();

  const cohortsState = useSWR(
    { type: "cohort", studyId, list: true },
    async () => await studySource.listCohorts(studyId, underlaySource)
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
    async () => await studySource.listFeatureSets(studyId, underlaySource)
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
    const cohort = await studySource.createCohort(
      underlay.name,
      studyId,
      `Untitled cohort ${new Date().toLocaleString()}`
    );
    navigate(cohortURL(cohort.id));
  };

  const newFeatureSet = async () => {
    const featureSet = await studySource.createFeatureSet(
      underlay.name,
      studyId,
      `Untitled feature set ${new Date().toLocaleString()}`
    );
    navigate(featureSetURL(featureSet.id));
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
        title="Data export"
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
                  <Loading status={cohortsState} size="medium">
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
                  </Loading>
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
                  <Loading status={featureSetsState} size="medium">
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
                            onClick={() => navigate(featureSetURL(fs.id))}
                            sx={{ minWidth: "auto" }}
                          >
                            Edit
                          </Button>
                        </GridLayout>
                      ))
                    )}
                  </Loading>
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
  cohorts: Cohort[];
  featureSets: FeatureSet[];
  selectedCohorts: Set<string>;
  selectedFeatureSets: Set<string>;
};

function Preview(props: PreviewProps) {
  const underlay = useUnderlay();

  const filteredCohorts = useMemo(
    () =>
      (props.cohorts ?? []).filter((cohort) =>
        props.selectedCohorts.has(cohort.id)
      ),
    [props.cohorts, props.selectedCohorts]
  );

  const filteredFeatureSets = useMemo(
    () =>
      props.featureSets.filter((fs) => props.selectedFeatureSets.has(fs.id)),
    [props.featureSets, props.selectedFeatureSets]
  );

  const occurrenceFiltersState = useOccurrenceList(
    filteredCohorts,
    filteredFeatureSets
  );

  const [exportDialog, showExportDialog] = useExportDialog({
    cohorts: filteredCohorts.map((c) => c.id),
    featureSets: filteredFeatureSets.map((fs) => fs.id),
    occurrenceFilters: occurrenceFiltersState.data ?? [],
  });

  const empty =
    props.selectedCohorts.size === 0 || props.selectedFeatureSets.size === 0;

  const onExportClick = () =>
    underlay.uiConfiguration.featureConfig?.overrideExportButton
      ? window.parent.postMessage(
          {
            message: "EXPORT",
            resources: {
              cohorts: filteredCohorts.map((c) => c.id),
              featureSets: filteredFeatureSets.map((fs) => fs.id),
            },
          },
          getEnvironment().REACT_APP_POST_MESSAGE_ORIGIN ??
            window.location.origin
        )
      : showExportDialog();

  return (
    <Loading status={occurrenceFiltersState}>
      <TanagraTabs
        configs={[
          {
            id: "summary",
            title: "Summary",
            render: () => (
              <PreviewSummary
                cohorts={filteredCohorts}
                featureSets={filteredFeatureSets.map((fs) => fs.id)}
                occurrenceFilters={occurrenceFiltersState.data ?? []}
                empty={empty}
              />
            ),
          },
          {
            id: "tables",
            title: "Data",
            render: () => (
              <PreviewTable
                cohorts={filteredCohorts.map((c) => c.id)}
                featureSets={filteredFeatureSets.map((fs) => fs.id)}
                occurrenceFilters={occurrenceFiltersState.data ?? []}
                empty={empty}
              />
            ),
          },
        ]}
        center
        hideDivider
        tabsPrefix={
          <GridBox sx={{ width: 200 }}>
            <Typography variant="body1em">Preview</Typography>
          </GridBox>
        }
        tabsSuffix={
          <GridLayout cols colAlign="right" sx={{ width: 200 }}>
            <Button
              variant="contained"
              disabled={
                empty ||
                underlay.uiConfiguration.featureConfig?.disableExportButton
              }
              onClick={() => {
                onExportClick();
              }}
            >
              Export dataset
            </Button>
          </GridLayout>
        }
      />
      {exportDialog}
    </Loading>
  );
}

type PreviewTabData = {
  name: string;
  sql: string;
  data: TreeGridData;
};

type PreviewTableProps = {
  cohorts: string[];
  featureSets: string[];
  occurrenceFilters: OccurrenceFilters[];
  empty: boolean;
};

function PreviewTable(props: PreviewTableProps) {
  const underlaySource = useUnderlaySource();
  const underlay = useUnderlay();
  const studyId = useStudyId();

  const [tab, setTab] = useState(0);
  const [queriesMode, setQueriesMode] = useState<boolean | null>(false);

  const tabDataState = useSWRImmutable<PreviewTabData[]>(
    {
      type: "previewData",
      occurrences: props.occurrenceFilters,
    },
    async () => {
      return Promise.all(
        props.occurrenceFilters.map(async (filters) => {
          const res = await underlaySource.exportPreview(
            underlay.name,
            filters.id,
            studyId,
            props.cohorts,
            props.featureSets
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
            sql: filters.sql ?? res.sql,
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
              spacing={1}
              sx={{ borderRight: 1, borderColor: "divider" }}
            >
              <GridLayout rows colAlign="center" sx={{ px: 1 }}>
                <ToggleButtonGroup
                  value={queriesMode}
                  exclusive
                  onChange={onQueriesModeChange}
                  sx={{ width: "auto" }}
                >
                  <ToggleButton value={false}>Tables</ToggleButton>
                  <ToggleButton value={true}>Queries</ToggleButton>
                </ToggleButtonGroup>
              </GridLayout>
              <GridLayout rows colAlign="right">
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
            </GridLayout>
            <GridBox>
              {queriesMode ? (
                <GridBox sx={{ overflow: "auto" }}>
                  <Typography
                    sx={{ whiteSpace: "pre", fontFamily: "monospace" }}
                  >
                    {tabDataState.data?.[tab]?.sql}
                  </Typography>
                </GridBox>
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
  cohorts: Cohort[];
  featureSets: string[];
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
          <GridLayout rows spacing={1} height="auto">
            <Typography variant="body1em">
              Here are the {props.occurrenceFilters.length} tables you get in
              your export
            </Typography>
            {props.occurrenceFilters.map((filters) => (
              <GridBox
                key={filters.id}
                sx={{
                  p: 1,
                  backgroundColor: (theme) => theme.palette.background.default,
                  borderRadius: (theme) => `${theme.shape.borderRadius}px`,
                }}
              >
                <OccurrenceFiltersSummary filters={filters} />
              </GridBox>
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
  cohorts: string[];
  featureSets: string[];
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

enum ExportDialogStage {
  MODEL_SELECT,
  EXPORTING,
  LINKS,
  REDIRECT_ERRORS,
  REDIRECT,
}

function ExportDialog(
  props: ExportDialogProps & { open: boolean; hide: () => void }
) {
  const underlaySource = useUnderlaySource();
  const underlay = useUnderlay();
  const studyId = useStudyId();
  const params = useBaseParams();

  const [stage, setStage] = useState(ExportDialogStage.MODEL_SELECT);
  const [modelId, setModelId] = useState<string>("");

  const exportModelsState = useSWRImmutable<ExportModel[]>(
    {
      type: "exportModel",
      underlayName: underlay.name,
    },
    async () => {
      return await underlaySource.listExportModels(underlay.name);
    }
  );

  const onSelectModel = (event: SelectChangeEvent<string>) => {
    const {
      target: { value: sel },
    } = event;
    setModelId(sel);
  };

  const model =
    exportModelsState.data?.find((m) => m.id === modelId) ??
    exportModelsState.data?.[0];

  // There seems to be an issue with the way useSWRMutation captures the
  // function that prevents it from updating when state changes, even if the
  // function is contained in a useCallback. Pass the data through the key
  // instead.
  const exportState = useSWRMutation(
    {
      type: "export",
      underlayName: underlay.name,
      modelId: model?.id,
      cohorts: props.cohorts,
      featureSets: props.featureSets,
      occurrenceFilters: props.occurrenceFilters,
    },
    async (key) => {
      if (!key.modelId) {
        throw new Error("Export models not loaded.");
      }

      const res = await underlaySource.export(
        underlay.name,
        studyId,
        key.modelId,
        RETURN_URL_PLACEHOLDER,
        key.cohorts,
        key.featureSets
      );

      if (res.redirectURL) {
        const errors = res.links.reduce(
          (cur, link) => cur || !!link.error,
          false
        );
        setStage(
          errors
            ? ExportDialogStage.REDIRECT_ERRORS
            : ExportDialogStage.REDIRECT
        );

        if (!errors) {
          redirect(res.redirectURL, absoluteExportURL(params));
        }
      } else {
        setStage(ExportDialogStage.LINKS);

        const download = (url: string) => {
          const tempLink = document.createElement("a");
          tempLink.setAttribute("href", url);
          tempLink.click();
          tempLink.remove();
        };

        for (let i = 0; i < res.links.length; i++) {
          const url = res.links[i].url;
          if (!url) {
            continue;
          }
          setTimeout(() => download(url), i * 300);
        }
      }

      return res;
    }
  );

  const orderedLinks = useMemo(() => {
    if (!exportState.data) {
      return [];
    }

    const orderedLinks: ExportResultLink[][] = [];
    for (const link of exportState.data.links) {
      const list = orderedLinks.find(
        (list) => list[0].tags[0] === link.tags[0]
      );
      if (list) {
        list.push(link);
      } else {
        orderedLinks.push([link]);
      }
    }

    orderedLinks.sort((a, b) => a[0].tags[0].localeCompare(b[0].tags[0]));
    return orderedLinks;
  }, [exportState.data]);

  if (!props.open) {
    return null;
  }

  const renderLink = (link: ExportResultLink, stage: ExportDialogStage) => {
    const row = !link.error ? (
      <GridLayout cols spacing={0.5} rowAlign="middle" height="auto">
        {stage === ExportDialogStage.LINKS ? (
          <DownloadIcon sx={{ display: "flex" }} />
        ) : (
          <CheckCircleIcon
            sx={{
              display: "flex",
              color: (theme) => theme.palette.success.dark,
            }}
          />
        )}
        <Typography variant="body2">{link.displayName}</Typography>
      </GridLayout>
    ) : (
      <GridLayout cols spacing={0.5} rowAlign="middle" height="auto">
        <Tooltip title={link.error}>
          <InfoIcon
            sx={{ display: "flex", color: (theme) => theme.palette.error.dark }}
          />
        </Tooltip>
        <Typography variant="body2" sx={{ color: "#00000099" }}>
          {link.error}
        </Typography>
      </GridLayout>
    );

    if (!link.error && stage === ExportDialogStage.LINKS) {
      return (
        <Link href={link.url} key={link.displayName}>
          {row}
        </Link>
      );
    }
    return row;
  };

  return (
    <Dialog
      fullWidth
      maxWidth="md"
      aria-labelledby="export-dialog-title"
      open={props.open}
      onClose={(event: object, reason: string) => {
        if (reason !== "backdropClick") {
          props.hide();
          setStage(ExportDialogStage.MODEL_SELECT);
        }
      }}
    >
      <DialogTitle id="export-dialog-title">
        {!model || stage === ExportDialogStage.MODEL_SELECT
          ? "Export dataset"
          : model.displayName}
      </DialogTitle>
      <DialogContent>
        {stage === ExportDialogStage.MODEL_SELECT ? (
          <Loading status={exportModelsState}>
            <FormControl>
              <RadioGroup
                value={model?.id ?? ""}
                onChange={onSelectModel}
                sx={{
                  gap: 1,
                }}
              >
                {exportModelsState.data?.map((m) => (
                  <ModelRadio
                    key={m.id}
                    model={m}
                    select={() => setModelId(m.id)}
                  />
                ))}
              </RadioGroup>
            </FormControl>
          </Loading>
        ) : null}
        {stage === ExportDialogStage.EXPORTING ? (
          <GridBox sx={{ minHeight: 100 }}>
            <Loading
              status={exportState}
              showProgressOnMutate
              disableReloadButton
            />
          </GridBox>
        ) : null}
        {stage === ExportDialogStage.LINKS ||
        stage === ExportDialogStage.REDIRECT_ERRORS ? (
          <GridLayout rows spacing={1} height="auto">
            <Typography variant="body1">
              {stage === ExportDialogStage.LINKS
                ? "Click on the filenames to download if they do not start automatically"
                : "There were errors exporting one or more files"}
            </Typography>
            {orderedLinks.map((list) => (
              <GridLayout key={list[0].tags[0]} rows spacing={1} height="auto">
                {list[0].tags[0] ? (
                  <Typography variant="body1">{list[0].tags[0]}</Typography>
                ) : null}
                <GridLayout rows height="auto">
                  {list.map((link) => renderLink(link, stage))}
                </GridLayout>
              </GridLayout>
            ))}
          </GridLayout>
        ) : null}
        {stage === ExportDialogStage.REDIRECT ? (
          <GridBox sx={{ minHeight: 100 }}>
            <CenteredContent progress text="Redirecting..." />
          </GridBox>
        ) : null}
      </DialogContent>
      <DialogActions>
        <Button
          size="large"
          onClick={() => {
            props.hide();
            setStage(ExportDialogStage.MODEL_SELECT);
          }}
        >
          {stage === ExportDialogStage.MODEL_SELECT ? "Cancel" : "Close"}
        </Button>
        {stage === ExportDialogStage.MODEL_SELECT ? (
          <Button
            size="large"
            variant="contained"
            onClick={() => {
              setStage(ExportDialogStage.EXPORTING);
              exportState.trigger();
            }}
          >
            Export
          </Button>
        ) : null}
        {stage === ExportDialogStage.REDIRECT_ERRORS ? (
          <Button
            variant="contained"
            onClick={() => {
              setStage(ExportDialogStage.REDIRECT);
              if (exportState.data?.redirectURL) {
                redirect(
                  exportState.data.redirectURL,
                  absoluteExportURL(params)
                );
              }
            }}
          >
            Continue
          </Button>
        ) : null}
      </DialogActions>
    </Dialog>
  );
}

type ModelRadioProps = {
  model: ExportModel;
  select: () => void;
};

function ModelRadio(props: ModelRadioProps) {
  const radioGroup = useRadioGroup();
  const checked = radioGroup?.value === props.model.id;

  return (
    <GridBox
      sx={{
        height: "auto",
        "&:hover": {
          cursor: "pointer",
        },
      }}
      onClick={() => props.select()}
    >
      <GridLayout
        rows={2}
        rowAlign="middle"
        cols={2}
        height="auto"
        sx={{
          borderStyle: "solid",
          borderWidth: "1px",
          borderColor: (theme) =>
            checked ? theme.palette.primary.main : "transparent",
          borderRadius: (theme) => `${theme.shape.borderRadius}px`,
        }}
      >
        <Radio value={props.model.id} />
        <Typography variant="body1em">{props.model.displayName}</Typography>
        <GridBox />
        <Typography
          variant="body2"
          sx={{
            overflowWrap: "break-word",
            wordBreak: "normal",
          }}
        >
          {props.model.description}
        </Typography>
      </GridLayout>
    </GridBox>
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
