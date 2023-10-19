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
import {
  getCriteriaPlugin,
  getCriteriaTitle,
  getOccurrenceList,
  OccurrenceFilters,
} from "cohort";
import Checkbox from "components/checkbox";
import Empty from "components/empty";
import Loading from "components/loading";
import { usePopover } from "components/popover";
import { SaveStatus } from "components/saveStatus";
import { Tabs } from "components/tabs";
import { useTextInputDialog } from "components/textInputDialog";
import { TreeGrid, TreeGridData } from "components/treegrid";
import { Filter, FilterType, makeArrayFilter } from "data/filter";
import { useSource } from "data/sourceContext";
import {
  deleteFeatureSetCriteria,
  deletePredefinedFeatureSetCriteria,
  setExcludedFeatureSetColumns,
  toggleFeatureSetColumn,
  updateFeatureSet,
  useFeatureSetContext,
} from "featureSet/featureSetContext";
import { useFeatureSet, useStudyId, useUnderlay } from "hooks";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useMemo, useState } from "react";
import {
  absoluteCohortURL,
  featureSetCriteriaURL,
  featureSetURL,
  useBaseParams,
  useExitAction,
} from "router";
import { StudyName } from "studyName";
import useSWRImmutable from "swr/immutable";
import * as tanagraUI from "tanagra-ui";
import UndoRedoToolbar from "undoRedoToolbar";
import { useGlobalSearchState, useNavigate } from "util/searchState";

export function FeatureSet() {
  const context = useFeatureSetContext();
  const featureSet = useFeatureSet();
  const exit = useExitAction();
  const underlay = useUnderlay();
  const source = useSource();
  const navigate = useNavigate();
  const params = useBaseParams();
  const studyId = useStudyId();

  const [renameTitleDialog, showRenameTitleDialog] = useTextInputDialog();

  const newCohort = async () => {
    const cohort = await source.createCohort(underlay.name, studyId);
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
          >
            <EditIcon />
          </IconButton>
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
  criteria?: tanagraUI.UICriteria;
};

function FeatureList() {
  const underlay = useUnderlay();
  const featureSet = useFeatureSet();
  const navigate = useNavigate();

  const addURL = `../${featureSetURL(featureSet.id)}/add`;

  const predefinedCriteria = underlay.uiConfiguration.prepackagedConceptSets;

  const sortedCriteria = useMemo(() => {
    const criteria: FeatureListCriteria[] = [];
    predefinedCriteria.forEach((c) => {
      if (featureSet.predefinedCriteria.indexOf(c.id) >= 0) {
        criteria.push({
          id: c.id,
          title: c.name,
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
  }, [predefinedCriteria, featureSet.criteria]);

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

  const plugin = !!props.criteria.criteria
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
      >
        <EditIcon />
      </IconButton>
      <IconButton
        onClick={() =>
          !!props.criteria.criteria
            ? deleteFeatureSetCriteria(context, props.criteria.id)
            : deletePredefinedFeatureSetCriteria(context, props.criteria.id)
        }
      >
        <DeleteIcon />
      </IconButton>
      <GridBox />
    </GridLayout>
  );
}

type PreviewTabData = {
  name: string;
  data: TreeGridData;
};

function Preview() {
  const featureSet = useFeatureSet();
  const underlay = useUnderlay();
  const source = useSource();
  const navigate = useNavigate();

  const occurrenceList = useMemo(() => {
    const selectedCriteria = new Set<string>();
    underlay.uiConfiguration.prepackagedConceptSets?.forEach((conceptSet) => {
      if (featureSet.predefinedCriteria.includes(conceptSet.id)) {
        selectedCriteria.add(conceptSet.id);
      }
    });

    featureSet.criteria.forEach((criteria) => {
      selectedCriteria.add(criteria.id);
    });

    return getOccurrenceList(
      source,
      selectedCriteria,
      featureSet.criteria,
      underlay.uiConfiguration.prepackagedConceptSets
    );
  }, [featureSet.criteria, featureSet.predefinedCriteria]);

  // TODO(tjennison): Look at supporting a "true" filter instead.
  const cohortFilter: Filter = {
    type: FilterType.Attribute,
    attribute: source.lookupOccurrence("").key,
    ranges: [{ min: Number.MIN_SAFE_INTEGER, max: Number.MAX_SAFE_INTEGER }],
  };

  const tabDataState = useSWRImmutable<PreviewTabData[]>(
    {
      type: "previewData",
      cohortFilter,
      occurrences: occurrenceList,
    },
    async () => {
      return Promise.all(
        occurrenceList.map(async (params) => {
          const res = await source.listData(
            params.attributes,
            params.id,
            cohortFilter,
            makeArrayFilter({ min: 1 }, params.filters)
          );

          const data: TreeGridData = {
            root: { data: {}, children: [] },
          };

          res.data.forEach((entry, i) => {
            data[i] = { data: entry };
            data.root?.children?.push(i);
          });

          return {
            id: params.id,
            name: params.name,
            data: data,
          };
        })
      );
    }
  );

  return (
    <GridLayout rows sx={{ pt: 1 }}>
      {featureSet.criteria.length > 0 ||
      featureSet.predefinedCriteria.length > 0 ? (
        <Loading status={tabDataState}>
          <Tabs
            configs={
              tabDataState.data?.map((data, i) => ({
                id: data.name,
                title: data.name,
                render: () =>
                  data.data.root?.children?.length ? (
                    <PreviewTable occurrence={occurrenceList[i]} data={data} />
                  ) : (
                    <GridLayout cols rowAlign="middle">
                      <Empty
                        maxWidth="90%"
                        image="/empty.svg"
                        title="No data matched"
                        subtitle="No data in this table matched the specified cohorts and data features"
                      />
                    </GridLayout>
                  ),
              })) ?? []
            }
          />
        </Loading>
      ) : (
        <GridLayout cols rowAlign="middle">
          <Empty
            maxWidth="90%"
            image="/empty.svg"
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
  occurrence: OccurrenceFilters;
  data: PreviewTabData;
};

function PreviewTable(props: PreviewTableProps) {
  const context = useFeatureSetContext();
  const featureSet = useFeatureSet();
  const [globalSearchState, updateGlobalSearchState] = useGlobalSearchState();

  const output = featureSet.output.find(
    (o) => o.occurrence === props.occurrence.id
  );

  const columns = useMemo(() => {
    return props.occurrence.attributes
      .filter(
        (attribute) =>
          !globalSearchState.showSelectedColumnsOnly ||
          !output?.excludedColumns?.includes(attribute)
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
              checked={!output?.excludedColumns?.includes(attribute)}
              onChange={() =>
                toggleFeatureSetColumn(context, props.occurrence.id, attribute)
              }
            />
          ),
        };
      });
  }, [
    props.occurrence,
    featureSet.output,
    globalSearchState.showSelectedColumnsOnly,
  ]);

  const [columnSearch, setColumnSearch] = useState("");

  const columnSearchRE = useMemo(
    () => new RegExp(columnSearch, "i"),
    [columnSearch]
  );

  const [manageColumns, showManageColumns] = usePopover({
    children: (
      <GridBox sx={{ width: "300px", height: "auto" }}>
        <GridLayout rows>
          <GridBox sx={{ p: 1 }}>
            <TextField
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
                  checked={!output?.excludedColumns?.includes(attribute)}
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
      {output?.excludedColumns?.length !==
      props.occurrence.attributes.length ? (
        <TreeGrid
          data={props.data?.data}
          columns={columns}
          rowCustomization={() => {
            return columns.map((col, i) => ({
              column: i,
              backgroundSx: output?.excludedColumns?.includes(col.key)
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
