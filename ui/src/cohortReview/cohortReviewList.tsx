import AddIcon from "@mui/icons-material/Add";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import InsertChartIcon from "@mui/icons-material/InsertChart";
import RateReviewIcon from "@mui/icons-material/RateReview";
import RecentActorsIcon from "@mui/icons-material/RecentActors";
import WarningIcon from "@mui/icons-material/Warning";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import ListItemButton from "@mui/material/ListItemButton";
import Paper from "@mui/material/Paper";
import { SxProps, useTheme } from "@mui/material/styles";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { generateId } from "cohort";
import { useNewAnnotationDialog } from "cohortReview/newAnnotationDialog";
import { useNewReviewDialog } from "cohortReview/useNewReviewDialog";
import { useReviewAnnotations } from "cohortReview/reviewHooks";
import { CohortSummary } from "cohortSummary";
import Empty from "components/empty";
import Loading from "components/loading";
import LoadingOverlay from "components/loadingOverlay";
import SelectablePaper from "components/selectablePaper";
import { useSimpleDialog } from "components/simpleDialog";
import { Tabs } from "components/tabs";
import { useTextInputDialog } from "components/textInputDialog";
import { TreeGrid, TreeGridId } from "components/treeGrid";
import { useArrayAsTreeGridData } from "components/treeGridHelpers";
import { AnnotationType, CohortReview } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import deepEqual from "deep-equal";
import { DemographicCharts } from "demographicCharts";
import { useCohort, useStudyId } from "hooks";
import { produce } from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { useParams } from "react-router-dom";
import {
  absoluteCohortReviewListURL,
  absoluteCohortURL,
  useBaseParams,
} from "router";
import useSWR from "swr";
import { useNavigate } from "util/searchState";
import { RouterLink } from "components/routerLink";

type PendingItem = {
  id: string;
  displayName: string;
  size: number;
  lastModified: Date;
};

class ReviewListItem {
  constructor(
    public review?: CohortReview,
    public pending?: PendingItem
  ) {}

  isPending() {
    return !!this.pending;
  }

  id() {
    return this.pending?.id ?? this.review?.id;
  }

  displayName() {
    return this.pending?.displayName ?? this.review?.displayName;
  }

  size() {
    return this.pending?.size ?? this.review?.size;
  }

  lastModified() {
    return this.pending?.lastModified ?? this.review?.cohort?.lastModified;
  }
}

function wrapResults(results: CohortReview[]): ReviewListItem[] {
  return results.map((r) => new ReviewListItem(r));
}

export function CohortReviewList() {
  const params = useBaseParams();
  const cohort = useCohort();

  return (
    <GridLayout
      rows
      sx={{
        backgroundColor: (theme) => theme.palette.background.paper,
      }}
    >
      <ActionBar
        title={`Reviews for cohort ${cohort.name}`}
        backAction={absoluteCohortURL(params, cohort.id)}
      />
      <Tabs
        configs={[
          {
            id: "reviews",
            title: "Reviews",
            render: () => <Reviews />,
          },
          {
            id: "annotations",
            title: "Annotations",
            render: () => <Annotations />,
          },
        ]}
        center
      />
    </GridLayout>
  );
}

function Reviews() {
  const theme = useTheme();
  const studySource = useStudySource();
  const underlaySource = useUnderlaySource();
  const navigate = useNavigate();
  const params = useBaseParams();
  const { reviewId } = useParams<{ reviewId: string }>();
  const cohort = useCohort();

  const reviewsState = useSWR(
    { component: "CohortReviewList", cohortId: cohort.id },
    async () => {
      const res = wrapResults(
        await studySource.listCohortReviews(
          params.studyId,
          underlaySource,
          cohort.id
        )
      );
      return res;
    }
  );

  let selectedReview: CohortReview | undefined;
  reviewsState.data?.forEach((item) => {
    if (selectedReview || !item.review) {
      return;
    }
    if (!reviewId || item.id() === reviewId) {
      selectedReview = item.review;
    }
  });

  const selectedReviewUpToDate = deepEqual(
    selectedReview?.cohort?.groupSections,
    cohort?.groupSections
  );

  const onCreateNewReview = (name: string, size: number) => {
    reviewsState.mutate(
      async () => {
        await studySource.createCohortReview(
          params.studyId,
          underlaySource,
          cohort,
          name,
          size
        );
        const res = wrapResults(
          await studySource.listCohortReviews(
            params.studyId,
            underlaySource,
            cohort.id
          )
        );
        return res;
      },
      {
        optimisticData: [
          new ReviewListItem(undefined, {
            displayName: name,
            size,
            lastModified: cohort.lastModified,
            id: generateId(),
          }),
          ...(reviewsState?.data ?? []),
        ],
      }
    );
  };

  const [confirmDialog, showConfirmDialog] = useSimpleDialog();

  const onDeleteReview = () => {
    reviewsState.mutate(
      async () => {
        if (selectedReview?.id) {
          await studySource.deleteCohortReview(
            params.studyId,
            cohort.id,
            selectedReview.id
          );
          navigate(absoluteCohortReviewListURL(params, cohort.id));
        }
        return wrapResults(
          await studySource.listCohortReviews(
            params.studyId,
            underlaySource,
            cohort.id
          )
        );
      },
      {
        optimisticData: [...(reviewsState?.data ?? [])].filter(
          (item) => item.id() !== selectedReview?.id
        ),
      }
    );
  };

  const onRenameReview = (name: string) => {
    reviewsState.mutate(
      async () => {
        if (selectedReview?.id) {
          await studySource.renameCohortReview(
            params.studyId,
            underlaySource,
            cohort.id,
            selectedReview.id,
            name
          );
        }
        return wrapResults(
          await studySource.listCohortReviews(
            params.studyId,
            underlaySource,
            cohort.id
          )
        );
      },
      {
        optimisticData: produce(reviewsState?.data, (data) => {
          const sel = data?.find((item) => item.id() === selectedReview?.id);
          if (sel && sel.review) {
            sel.review.displayName = name;
          }
        }),
      }
    );
  };

  const [newReviewDialog, showNewReviewDialog] = useNewReviewDialog({
    cohort,
    onCreate: onCreateNewReview,
  });

  const [renameReviewDialog, showRenameReviewDialog] = useTextInputDialog();

  const emptyIconSx: SxProps = {
    p: 1,
    color: theme.palette.primary.main,
    backgroundColor: theme.palette.info.main,
    borderRadius: "50%",
    width: "2em",
    height: "2em",
  };

  return (
    <GridLayout cols="300px 1fr">
      <GridLayout
        rows
        sx={{
          boxShadow: (theme) => `inset -1px 0 0 ${theme.palette.divider}`,
        }}
      >
        <GridLayout
          cols
          fillCol={0}
          rowAlign="middle"
          height="auto"
          sx={{
            px: 3,
            py: 2,
            boxShadow: (theme) => `inset -1px -1px 0 ${theme.palette.divider}`,
          }}
        >
          <Typography variant="h6" sx={{ mr: 1 }}>
            {`Reviews (${reviewsState.data?.length ?? 0})`}
          </Typography>
          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={showNewReviewDialog}
          >
            New review
          </Button>
          <GridBox />
        </GridLayout>
        <GridBox
          sx={{
            backgroundColor: (theme) => theme.palette.background.default,
          }}
        >
          {reviewsState.data?.length ? (
            <GridLayout
              rows
              spacing={2}
              sx={{
                px: 3,
                py: 2,
                boxShadow: (theme) =>
                  `inset -1px -1px 0 ${theme.palette.divider}`,
              }}
            >
              {reviewsState.data?.map((item) => (
                <ListItemButton
                  sx={{
                    p: 0,
                    borderRadius: (theme) => `${theme.shape.borderRadius}px`,
                  }}
                  component={RouterLink}
                  key={item.id()}
                  to={absoluteCohortReviewListURL(params, cohort.id, item.id())}
                  disabled={!!item.pending}
                >
                  <SelectablePaper selected={item.id() === selectedReview?.id}>
                    <GridLayout rows sx={{ p: 1 }}>
                      <GridLayout cols fillCol={0}>
                        <ReviewChip item={item} />
                        {item.pending ? (
                          <CircularProgress
                            sx={{ maxWidth: "1em", maxHeight: "1em" }}
                          />
                        ) : null}
                      </GridLayout>
                      <Typography variant="body1em">
                        {item.displayName()}
                      </Typography>
                      <Typography variant="body1">
                        {item.lastModified()?.toLocaleString()}
                      </Typography>
                      <Typography variant="body1">
                        {`Participants: ${item.size()}`}
                      </Typography>
                    </GridLayout>
                  </SelectablePaper>
                </ListItemButton>
              ))}
            </GridLayout>
          ) : (
            <Empty
              maxWidth="80%"
              title="Your reviews will show up here"
              subtitle="Create a new review to get started"
            />
          )}
        </GridBox>
      </GridLayout>
      <GridBox>
        {selectedReview ? (
          <GridLayout rows>
            <GridLayout
              cols
              rowAlign="middle"
              colAlign="right"
              sx={{
                px: 3,
                py: 2,
              }}
            >
              <GridLayout rows>
                <GridLayout cols>
                  <Typography variant="h6" sx={{ mr: 1 }}>
                    {selectedReview.displayName}
                  </Typography>
                  <IconButton
                    onClick={() =>
                      showRenameReviewDialog({
                        title: `Rename ${selectedReview?.displayName}`,
                        initialText: selectedReview?.displayName,
                        textLabel: "Review name",
                        buttonLabel: "Rename",
                        onConfirm: onRenameReview,
                      })
                    }
                    size="small"
                  >
                    <EditIcon />
                  </IconButton>
                  <IconButton
                    onClick={() =>
                      showConfirmDialog({
                        title: `Delete ${selectedReview?.displayName}?`,
                        text: `Review "${selectedReview?.displayName}" will be deleted. Are you sure you want to continue?`,
                        buttons: ["Cancel", "Delete review"],
                        primaryButtonColor: "error",
                        onButton: (button) => {
                          if (button === 1) {
                            onDeleteReview();
                          }
                        },
                      })
                    }
                    size="small"
                  >
                    <DeleteIcon />
                  </IconButton>
                </GridLayout>
                <Typography variant="body1">{`Created by: ${selectedReview.createdBy}`}</Typography>
              </GridLayout>
              <Button
                variant="contained"
                size="large"
                onClick={() =>
                  navigate(
                    absoluteCohortReviewListURL(
                      params,
                      cohort.id,
                      selectedReview?.id
                    ) + "/review"
                  )
                }
              >
                Review individual participants
              </Button>
            </GridLayout>
            <ReviewStats
              review={selectedReview}
              upToDate={selectedReviewUpToDate}
            />
          </GridLayout>
        ) : (
          <Empty
            maxWidth="80%"
            minHeight="300px"
            title="Here you can review a subset of your cohort, and add notes & annotations"
            subtitle={
              <GridLayout rows spacing={4} height="auto">
                <GridLayout
                  cols
                  fillCol={-1}
                  spacing={4}
                  height="auto"
                  sx={{ mt: 2 }}
                >
                  <GridLayout rows colAlign="center" height="auto">
                    <RecentActorsIcon sx={emptyIconSx} />
                    <Typography variant="body1">
                      Review cohort participant data
                    </Typography>
                  </GridLayout>
                  <GridLayout rows colAlign="center" height="auto">
                    <InsertChartIcon sx={emptyIconSx} />
                    <Typography variant="body1">
                      View descriptive cohort statistics
                    </Typography>
                  </GridLayout>
                  <GridLayout rows colAlign="center" height="auto">
                    <RateReviewIcon sx={emptyIconSx} />
                    <Typography variant="body1">
                      Share notes with collaborators
                    </Typography>
                  </GridLayout>
                </GridLayout>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={showNewReviewDialog}
                >
                  New review
                </Button>
              </GridLayout>
            }
          />
        )}
        {confirmDialog}
        {newReviewDialog}
        {renameReviewDialog}
      </GridBox>
      <GridBox sx={{ gridArea: "1/1/-1/-1" }}>
        {reviewsState.isLoading ? <LoadingOverlay /> : undefined}
      </GridBox>
    </GridLayout>
  );
}

type ReviewChipProps = {
  item: ReviewListItem;
};

function ReviewChip(props: ReviewChipProps) {
  const cohort = useCohort();

  if (props.item.isPending()) {
    return (
      <Chip
        color="secondary"
        icon={<CheckCircleIcon />}
        variant="filled"
        label="Creating"
      />
    );
  }
  if (
    deepEqual(props.item?.review?.cohort?.groupSections, cohort?.groupSections)
  ) {
    return (
      <Chip
        color="success"
        icon={<CheckCircleIcon />}
        variant="filled"
        label="Latest"
      />
    );
  }
  return (
    <Chip
      color="warning"
      icon={<WarningIcon />}
      variant="filled"
      label="Outdated"
    />
  );
}

type ReviewStatsProps = {
  review: CohortReview;
  upToDate: boolean;
};

function ReviewStats(props: ReviewStatsProps) {
  return (
    <Tabs
      configs={[
        {
          id: "overview",
          title: "Overview",
          render: () => (
            <Summary review={props.review} upToDate={props.upToDate} />
          ),
        },
        {
          id: "charts",
          title: "Charts",
          render: () => (
            <GridBox
              sx={{
                px: 3,
                py: 2,
                backgroundColor: (theme) => theme.palette.background.default,
              }}
            >
              <DemographicCharts cohort={props.review.cohort} />
            </GridBox>
          ),
        },
      ]}
    />
  );
}

type SummaryProps = {
  review: CohortReview;
  upToDate: boolean;
};

function Summary(props: SummaryProps) {
  return (
    <GridLayout
      rows
      spacing={2}
      sx={{
        px: 3,
        py: 2,
        backgroundColor: (theme) => theme.palette.background.default,
      }}
    >
      <GridLayout rows>
        <Typography variant="h6">
          Cohort definition when review started
        </Typography>
        <GridLayout cols spacing={1}>
          {props.upToDate ? (
            <CheckCircleIcon color="success" />
          ) : (
            <WarningIcon color="warning" />
          )}
          <Typography variant="body2">
            {props.upToDate
              ? "Reviewing the most recent version of this cohort"
              : "Reviewing an outdated version of this cohort"}
          </Typography>
        </GridLayout>
      </GridLayout>
      <Paper sx={{ p: 1 }}>
        <CohortSummary cohort={props.review.cohort} />
      </Paper>
    </GridLayout>
  );
}

function Annotations() {
  const studySource = useStudySource();
  const studyId = useStudyId();
  const cohort = useCohort();

  const annotationsState = useReviewAnnotations();

  const [newAnnotationDialog, showNewAnnotationDialog] = useNewAnnotationDialog(
    {
      onCreate: async (
        displayName: string,
        annotationType: AnnotationType,
        enumVals?: string[]
      ) => {
        await studySource.createAnnotation(
          studyId,
          cohort.id,
          displayName,
          annotationType,
          enumVals
        );
        annotationsState.mutate();
      },
    }
  );

  const [confirmDialog, showConfirmDialog] = useSimpleDialog();
  const [renameDialog, showRenameDialog] = useTextInputDialog();

  const data = useArrayAsTreeGridData(
    annotationsState.data?.map((a) => ({
      ...a,
      type: a.enumVals?.length ? "Review status" : "Free text",
    })) ?? [],
    "id"
  );

  const columns = [
    { key: "displayName", width: "100%", title: "Name" },
    { key: "type", width: 200, title: "Type" },
    { key: "buttons", width: 200 },
  ];

  return (
    <Loading status={annotationsState}>
      <GridLayout
        rows
        spacing={2}
        sx={{
          px: 5,
          py: 3,
          backgroundColor: (theme) => theme.palette.background.default,
        }}
      >
        <GridLayout cols fillCol={0} rowAlign="middle">
          <GridLayout rows>
            <Typography variant="body1em">
              Annotation fields ({annotationsState.data?.length})
            </Typography>
            <Typography variant="body2">
              Fields you add here will appear in all reviews of this cohort.
              You’ll see annotation fields when reviewing individuals.
            </Typography>
          </GridLayout>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => showNewAnnotationDialog()}
          >
            Add annotation field
          </Button>
        </GridLayout>
        <Paper>
          {annotationsState.data?.length ? (
            <TreeGrid
              data={data}
              columns={columns}
              rowCustomization={(id: TreeGridId, { data: annotation }) => {
                return [
                  {
                    column: columns.length - 1,
                    content: (
                      <GridLayout cols fillCol={0}>
                        <GridBox />
                        <IconButton
                          onClick={() =>
                            showRenameDialog({
                              title: `Updating ${annotation.displayName} display name`,
                              textLabel: "Display name",
                              initialText: annotation.displayName,
                              buttonLabel: "Update",
                              onConfirm: async (name: string) => {
                                await studySource.updateAnnotation(
                                  studyId,
                                  cohort.id,
                                  annotation.id,
                                  name
                                );
                                annotationsState.mutate();
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
                              title: `Delete ${annotation.displayName}?`,
                              text: `All annotations in the field "${annotation.displayName}" will be deleted. Are you sure you want to continue?`,
                              buttons: ["Cancel", "Delete annotations"],
                              primaryButtonColor: "error",
                              onButton: async (button) => {
                                if (button === 1) {
                                  await studySource.deleteAnnotation(
                                    studyId,
                                    cohort.id,
                                    annotation.id
                                  );
                                  await annotationsState.mutate();
                                }
                              },
                            })
                          }
                          size="small"
                        >
                          <DeleteIcon />
                        </IconButton>
                      </GridLayout>
                    ),
                  },
                ];
              }}
            />
          ) : (
            <Empty
              maxWidth="80%"
              title="Add notes about individuals during cohort review"
              subtitle={
                <>
                  <Link
                    variant="link"
                    underline="hover"
                    onClick={() => showNewAnnotationDialog()}
                    sx={{ cursor: "pointer" }}
                  >
                    Add an annotation field
                  </Link>{" "}
                  to get started
                </>
              }
            />
          )}
        </Paper>
      </GridLayout>
      {newAnnotationDialog}
      {confirmDialog}
      {renameDialog}
    </Loading>
  );
}
