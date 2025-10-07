import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import ViewColumnIcon from "@mui/icons-material/ViewColumn";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import Paper from "@mui/material/Paper";
import Step from "@mui/material/Step";
import StepConnector from "@mui/material/StepConnector";
import StepLabel from "@mui/material/StepLabel";
import Switch from "@mui/material/Switch";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { getCriteriaPlugin, getCriteriaTitle, useOccurrenceList } from "cohort";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import Loading from "components/loading";
import { usePopover } from "components/popover";
import { SaveStatus } from "components/saveStatus";
import { useSimpleDialog } from "components/simpleDialog";
import { Tabs } from "components/tabs";
import { useTextInputDialog } from "components/textInputDialog";
import { TreeGrid, TreeGridData, TreeGridId } from "components/treeGrid";
import { Criteria } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import {
  deleteFeatureSetCriteria,
  deletePredefinedFeatureSetCriteria,
  setExcludedFeatureSetColumns,
  toggleFeatureSetColumn,
  updateFeatureSet,
  useFeatureSetContext,
  useFeatureSetPreviewContext,
} from "featureSet/featureSetContext";
import { useFeatureSet, useStudyId, useUnderlay } from "hooks";
import emptyImage from "images/empty.svg";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useEffect, useMemo, useState } from "react";
import {
  absoluteCohortURL,
  featureSetCriteriaURL,
  featureSetURL,
  useBaseParams,
  useExitAction,
} from "router";
import { StudyName } from "studyName";
import UndoRedoToolbar from "undoRedoToolbar";
import { safeRegExp } from "util/safeRegExp";
import { useGlobalSearchState, useNavigate } from "util/searchState";

export function FeatureSet() {
  const context = useFeatureSetContext();
  const featureSet = useFeatureSet();
  const exit = useExitAction();
  const underlay = useUnderlay();
  const studySource = useStudySource();
  const navigate = useNavigate();
  const params = useBaseParams();
  const studyId = useStudyId();

  const [renameTitleDialog, showRenameTitleDialog] = useTextInputDialog();
  const [confirmDialog, showConfirmDialog] = useSimpleDialog();

  const newCohort = async () => {
    const cohort = await studySource.createCohort(underlay.name, studyId);
    navigate(absoluteCohortURL(params, cohort.id));
  };

  return (
    <GridLayout rows>
      <ActionBar
        title={featureSet.name}
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
          <GridLayout cols>
            <IconButton
              onClick={() =>
                showRenameTitleDialog({
                  title: "Editing feature set name",
                  initialText: featureSet.name,
                  textLabel: "FeatureSet name",
                  buttonLabel: "Update",
                  onConfirm: (name: string) => {
                    updateFeatureSet(context, name);
                  },
                })
              }
              size="small"
            >
              <EditIcon />
            </IconButton>
            <IconButton
              onClick={() =>
                showConfirmDialog({
                  title: `Delete ${featureSet.name}?`,
                  text: `Are you sure you want to delete "${featureSet.name}"? This action is permanent.`,
                  buttons: ["Cancel", "Delete"],
                  onButton: async (button) => {
                    if (button === 1) {
                      await studySource.deleteFeatureSet(
                        studyId,
                        featureSet.id
                      );
                      exit();
                    }
                  },
                })
              }
              size="small"
            >
              <DeleteIcon />
            </IconButton>
          </GridLayout>
        }
        rightControls={<UndoRedoToolbar />}
        backAction={exit}
      />
      <GridLayout rows spacing={3} sx={{ px: 5, py: 3 }}>
        <GridLayout cols="380px auto" spacing={2} rowAlign="middle">
          <GridLayout cols>
            <Step index={0} active>
              <StepLabel>Add data features of interest</StepLabel>
            </Step>
            <StepConnector />
          </GridLayout>
          <GridLayout cols fillCol={1}>
            <Step
              index={1}
              active={featureSet.criteria.length > 0}
              disabled={featureSet.criteria.length === 0}
            >
              <StepLabel>Preview and manage columns</StepLabel>
            </Step>
            <GridBox />
            <Button
              startIcon={<AddIcon />}
              onClick={() => newCohort()}
              variant="outlined"
            >
              New cohort
            </Button>
          </GridLayout>
        </GridLayout>
        <GridLayout cols="380px auto" spacing={2}>
          <Paper sx={{ height: "100%" }}>
            <FeatureList />
            {renameTitleDialog}
            {confirmDialog}
          </Paper>
          <Paper sx={{ height: "100%" }}>
            <Preview />
          </Paper>
        </GridLayout>
      </GridLayout>
    </GridLayout>
  );
}

type FeatureListCriteria = {
  id: string;
  title: string;
  criteria?: Criteria;
};

function FeatureList() {
  const underlay = useUnderlay();
  const featureSet = useFeatureSet();
  const navigate = useNavigate();

  const addURL = `../${featureSetURL(featureSet.id)}/add`;

  const predefinedCriteria = underlay.prepackagedDataFeatures;

  const sortedCriteria = useMemo(() => {
    const criteria: FeatureListCriteria[] = [];
    predefinedCriteria.forEach((c) => {
      if (featureSet.predefinedCriteria.indexOf(c.name) >= 0) {
        criteria.push({
          id: c.name,
          title: c.displayName,
        });
      }
    });

    featureSet.criteria.forEach((c) => {
      criteria.push({
        id: c.id,
        title: getCriteriaTitle(c),
        criteria: c,
      });
    });

    return criteria.sort((a, b) => a.title.localeCompare(b.title));
  }, [predefinedCriteria, featureSet.criteria, featureSet.predefinedCriteria]);

  return (
    <GridBox sx={{ px: 2, py: 1 }}>
      <GridLayout rows spacing={2}>
        {!sortedCriteria.length ? (
          <GridLayout cols rowAlign="middle">
            <Empty
              maxWidth="90%"
              title="Select the data features of interest to your research"
              subtitle={
                <>
                  <Link
                    variant="link"
                    underline="hover"
                    onClick={() => navigate(addURL)}
                    sx={{ cursor: "pointer" }}
                  >
                    Add a data feature
                  </Link>{" "}
                  to get started
                </>
              }
            />
          </GridLayout>
        ) : (
          <GridLayout rows height="auto" sx={{ overflowY: "auto" }}>
            {sortedCriteria.map((c) => (
              <GridBox key={c.id} sx={{ pb: 1 }}>
                <FeatureSetCriteria criteria={c} />
                <Divider />
              </GridBox>
            ))}
            <Button variant="contained" onClick={() => navigate(addURL)}>
              Add data feature
            </Button>
          </GridLayout>
        )}
      </GridLayout>
    </GridBox>
  );
}

type FeatureSetCriteriaProps = {
  criteria: FeatureListCriteria;
};

function FeatureSetCriteria(props: FeatureSetCriteriaProps) {
  const context = useFeatureSetContext();
  const navigate = useNavigate();

  const plugin = props.criteria.criteria
    ? getCriteriaPlugin(props.criteria.criteria)
    : undefined;

  return (
    <GridLayout cols rowAlign="middle" sx={{ px: 1 }}>
      <Typography variant="body1" title={props.criteria.title}>
        {props.criteria.title}
      </Typography>
      <IconButton
        disabled={!plugin?.renderEdit}
        onClick={() => navigate(featureSetCriteriaURL(props.criteria.id))}
        size="small"
      >
        <EditIcon />
      </IconButton>
      <IconButton
        onClick={() =>
          props.criteria.criteria
            ? deleteFeatureSetCriteria(context, props.criteria.id)
            : deletePredefinedFeatureSetCriteria(context, props.criteria.id)
        }
        size="small"
      >
        <DeleteIcon />
      </IconButton>
      <GridBox />
    </GridLayout>
  );
}

type PreviewOccurrence = {
  id: string;
  attributes: string[];
  sourceCriteria: string[];
};

export type PreviewTabData = {
  name: string;
  data: TreeGridData;
  sourceCriteria: string[];
};

function Preview() {
  const {
    previewData,
    updatePreviewData,
    updating,
    setUpdating,
    currentTab,
    setCurrentTab,
  } = useFeatureSetPreviewContext();
  const featureSet = useFeatureSet();
  const underlaySource = useUnderlaySource();
  const underlay = useUnderlay();
  const studyId = useStudyId();
  const navigate = useNavigate();

  const occurrenceFiltersState = useOccurrenceList([], [featureSet], true);

  // Prevent the preview data from being refetched if the list of entities
  // hasn't changed (i.e. an attribute was toggled).
  const [previewOccurrences, setPreviewOccurrences] = useState<
    PreviewOccurrence[]
  >([]);
  useEffect(() => {
    if (occurrenceFiltersState.data) {
      const newPreviewOccurrences =
        occurrenceFiltersState.data?.map((of) => ({
          id: of.id,
          attributes: of.attributes,
          sourceCriteria: of.sourceCriteria,
        })) ?? [];
      newPreviewOccurrences.sort((a, b) => a.id.localeCompare(b.id));
      let previewOccurrencesToLoad: PreviewOccurrence[] = [];
      let updateExisting = false;
      if (newPreviewOccurrences.length > previewData?.length) {
        previewOccurrencesToLoad = newPreviewOccurrences.filter(
          (npo) => !previewData.some((po) => po.name === npo.id)
        );
      } else if (
        newPreviewOccurrences.length === previewData?.length
      ) {
        const updatedPreviewOccurrences = newPreviewOccurrences.filter(
          (npo, n) =>
            JSON.stringify(npo.sourceCriteria) !==
            JSON.stringify(previewData[n].sourceCriteria)
        );
        if (updatedPreviewOccurrences.length > 0) {
          previewOccurrencesToLoad = updatedPreviewOccurrences;
          updatePreviewData(
            previewData.map((pd) => {
              const updatedIndex = updatedPreviewOccurrences.findIndex(
                (upo) => upo.id === pd.name
              );
              if (updatedIndex > -1) {
                return {
                  ...pd,
                  sourceCriteria:
                    updatedPreviewOccurrences[updatedIndex].sourceCriteria,
                };
              } else {
                return pd;
              }
            })
          );
          updateExisting = true;
        }
      } else if (
        newPreviewOccurrences.length < previewData?.length
      ) {
        const newPreviewData = previewData.filter((pd) =>
          newPreviewOccurrences.some((npo) => npo.id === pd.name)
        );
        updatePreviewData(newPreviewData);
      }
      if (previewOccurrencesToLoad?.length > 0) {
        loadNewPreviewData(
          previewOccurrencesToLoad,
          newPreviewOccurrences,
          updateExisting
        );
      } else if (
        newPreviewOccurrences.length > 0 &&
        previewOccurrences.length === 0
      ) {
        setPreviewOccurrences(newPreviewOccurrences);
      }
    }
    // Removed loadNewPreviewData from the deps array since it may not be initialized yet and  causes separate lint error
    // Removed previewContext from the deps array to prevent redundant previewExport calls
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [occurrenceFiltersState.data, previewOccurrences.length]);

  const loadNewPreviewData = async (
    newOccurrences: PreviewOccurrence[],
    allOccurrences: PreviewOccurrence[],
    updateExisting?: boolean
  ) => {
    setUpdating(true);
    const newOccurrenceData = await Promise.all(
      newOccurrences.map(async (params) => {
        const res = await underlaySource.exportPreview(
          underlay.name,
          params.id,
          studyId,
          [],
          [featureSet.id],
          true
        );

        const children: TreeGridId[] = [];
        const rows = new Map();

        res.data.forEach((entry, i) => {
          rows.set(i, { data: entry });
          children?.push(i);
        });

        return {
          id: params.id,
          name: params.id,
          data: {
            rows,
            children,
          },
          attributes: params.attributes,
          sourceCriteria: params.sourceCriteria,
        };
      })
    );
    if (!updateExisting) {
      const updatedPreviewData = [
        ...previewData,
        ...newOccurrenceData,
      ];
      updatedPreviewData.sort((a, b) => a.name.localeCompare(b.name));
      updatePreviewData(updatedPreviewData);
      if (!currentTab) {
        setCurrentTab(updatedPreviewData[0].name);
      }
    }
    setUpdating(false);
    allOccurrences.sort((a, b) => a.id.localeCompare(b.id));
    setPreviewOccurrences(allOccurrences);
  };

  return (
    <GridLayout rows sx={{ pt: 1 }}>
      {featureSet.criteria.length > 0 ||
      featureSet.predefinedCriteria.length > 0 ? (
        <Loading
          status={{}}
          isLoading={
            occurrenceFiltersState.isLoading || updating
          }
          showLoadingMessage={true}
        >
          <Tabs
            configs={
              (previewData ?? []).map((data) => ({
                id: data.name,
                title: data.name,
                render: () => {
                  const previewOccurrence = previewOccurrences.find(
                    (po) => po.id === data.name
                  );
                  return data.data.children?.length ? (
                    previewOccurrence ? (
                      <PreviewTable
                        occurrence={previewOccurrence}
                        data={data}
                      />
                    ) : null
                  ) : (
                    <GridLayout cols rowAlign="middle">
                      <Empty
                        maxWidth="90%"
                        image={emptyImage}
                        title="No data matched"
                        subtitle="No data in this table matched the specified cohorts and data features"
                      />
                    </GridLayout>
                  );
                },
              })) ?? []
            }
            currentTab={currentTab}
            setCurrentTab={setCurrentTab}
          />
        </Loading>
      ) : (
        <GridLayout cols rowAlign="middle">
          <Empty
            maxWidth="90%"
            image={emptyImage}
            title="Set up columns for each data feature here. You’ll see a preview of your table."
            subtitle={
              <>
                <Link
                  variant="link"
                  underline="hover"
                  onClick={() =>
                    navigate(`../${featureSetURL(featureSet.id)}/add`)
                  }
                  sx={{ cursor: "pointer" }}
                >
                  Add a data feature
                </Link>{" "}
                to start tinkering. Your columns will be saved as part of the
                data feature set.
              </>
            }
          />
        </GridLayout>
      )}
    </GridLayout>
  );
}

type PreviewTableProps = {
  occurrence: PreviewOccurrence;
  data: PreviewTabData;
};

function PreviewTable(props: PreviewTableProps) {
  const context = useFeatureSetContext();
  const featureSet = useFeatureSet();
  const [globalSearchState, updateGlobalSearchState] = useGlobalSearchState();

  const output = useMemo(
    () => featureSet.output.find((o) => o.occurrence === props.occurrence.id),
    [featureSet.output, props.occurrence.id]
  );

  const columns = useMemo(() => {
    return props.occurrence.attributes
      .filter(
        (attribute) =>
          !globalSearchState.showSelectedColumnsOnly ||
          !output?.excludedAttributes?.includes(attribute)
      )
      .map((attribute) => {
        return {
          key: attribute,
          width: 200,
          title: attribute,
          suffixElements: (
            <Checkbox
              name={attribute}
              size="small"
              fontSize="small"
              checked={!output?.excludedAttributes?.includes(attribute)}
              onChange={() =>
                toggleFeatureSetColumn(context, props.occurrence.id, attribute)
              }
            />
          ),
        };
      });
  }, [
    props.occurrence,
    output,
    globalSearchState.showSelectedColumnsOnly,
    context,
  ]);

  const [columnSearch, setColumnSearch] = useState("");

  const columnSearchRE = useMemo(
    () => safeRegExp(columnSearch)[0],
    [columnSearch]
  );

  const [manageColumns, showManageColumns] = usePopover({
    children: (
      <GridBox sx={{ width: "300px", height: "auto" }}>
        <GridLayout rows>
          <GridBox sx={{ p: 1 }}>
            <TextField
              variant="outlined"
              size="small"
              fullWidth
              label="Find column"
              value={columnSearch}
              onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                setColumnSearch(event.target.value);
              }}
            />
          </GridBox>
          {props.occurrence.attributes
            .filter(
              (attribute) => !columnSearch || attribute.match(columnSearchRE)
            )
            .map((attribute) => (
              <GridLayout key={attribute} cols>
                <Switch
                  name={attribute}
                  size="small"
                  checked={!output?.excludedAttributes?.includes(attribute)}
                  onChange={() =>
                    toggleFeatureSetColumn(
                      context,
                      props.occurrence.id,
                      attribute
                    )
                  }
                />
                <Typography variant="body2">{attribute}</Typography>
              </GridLayout>
            ))}
          <GridLayout cols fillCol={1}>
            <Button
              size="medium"
              onClick={() =>
                setExcludedFeatureSetColumns(
                  context,
                  props.occurrence.id,
                  props.occurrence.attributes
                )
              }
            >
              Exclude all
            </Button>
            <GridBox />
            <Button
              size="medium"
              onClick={() =>
                setExcludedFeatureSetColumns(context, props.occurrence.id, [])
              }
            >
              Include all
            </Button>
          </GridLayout>
        </GridLayout>
      </GridBox>
    ),
    PaperProps: {
      square: true,
    },
  });

  return (
    <GridLayout rows>
      <GridLayout cols fillCol={1} sx={{ px: 2, py: 1 }}>
        <Button
          size="medium"
          startIcon={<ViewColumnIcon />}
          onClick={showManageColumns}
        >
          Manage columns
        </Button>
        <GridBox>{manageColumns}</GridBox>
        <Switch
          name="show-included-columns-only"
          checked={globalSearchState.showSelectedColumnsOnly}
          onChange={() => {
            updateGlobalSearchState((state) => {
              state.showSelectedColumnsOnly = !state.showSelectedColumnsOnly;
            });
          }}
          size="small"
        />
        <Typography variant="body1">Show included columns only</Typography>
      </GridLayout>
      {output?.excludedAttributes?.length !==
      props.occurrence.attributes.length ? (
        <TreeGrid
          data={props.data?.data}
          columns={columns}
          rowCustomization={() => {
            return columns.map((col, i) => ({
              column: i,
              backgroundSx: output?.excludedAttributes?.includes(col.key)
                ? {
                    backgroundColor: (theme) =>
                      theme.palette.background.default,
                  }
                : undefined,
            }));
          }}
        />
      ) : (
        <Empty
          maxWidth="90%"
          title="No columns are included"
          subtitle={"Select columns to include using the Manage columns button"}
        />
      )}
    </GridLayout>
  );
}
