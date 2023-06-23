import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { generateId } from "cohort";
import { useCohortContext } from "cohortContext";
import Empty from "components/empty";
import LoadingOverlay from "components/loadingOverlay";
import SelectablePaper from "components/selectablePaper";
import { useTextInputDialog } from "components/textInputDialog";
import { useSource } from "data/sourceContext";
import { CohortReview } from "data/types";
import { DemographicCharts } from "demographicCharts";
import produce from "immer";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";
import {
  absoluteCohortReviewListURL,
  absoluteCohortURL,
  useBaseParams,
} from "router";
import useSWR from "swr";
import { useNewReviewDialog } from "./newReviewDialog";

type PendingItem = {
  id: string;
  displayName: string;
  size: number;
  created: Date;
};

class ReviewListItem {
  constructor(public review?: CohortReview, public pending?: PendingItem) {}

  id() {
    return this.pending?.id ?? this.review?.id;
  }

  displayName() {
    return this.pending?.displayName ?? this.review?.displayName;
  }

  size() {
    return this.pending?.size ?? this.review?.size;
  }

  created() {
    return this.pending?.created ?? this.review?.created;
  }
}

function wrapResults(results: CohortReview[]): ReviewListItem[] {
  return results.map((r) => new ReviewListItem(r));
}

function firstReview(list: ReviewListItem[]) {
  return list.reduce<CohortReview | undefined>(
    (cur, next) => cur ?? next.review,
    undefined
  );
}

export function CohortReviewList() {
  const source = useSource();
  const navigate = useNavigate();
  const params = useBaseParams();
  const { reviewId } = useParams<{ reviewId: string }>();

  const cohort = useCohortContext().state?.present;
  if (!cohort) {
    throw new Error("Cohort context state is null.");
  }

  const reviewsState = useSWR(
    { component: "CohortReviewList", cohortId: cohort.id },
    async () => {
      const res = wrapResults(
        await source.listCohortReviews(params.studyId, cohort.id)
      );
      if (!reviewId) {
        const first = firstReview(res);
        if (first) {
          navigate(first.id, { replace: true });
        }
      }
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

  const onCreateNewReview = (name: string, size: number) => {
    reviewsState.mutate(
      async () => {
        await source.createCohortReview(params.studyId, cohort, name, size);
        const res = wrapResults(
          await source.listCohortReviews(params.studyId, cohort.id)
        );
        if (!reviewId) {
          const first = firstReview(res);
          if (first) {
            navigate(first.id, { replace: true });
          }
        }
        return res;
      },
      {
        optimisticData: [
          new ReviewListItem(undefined, {
            displayName: name,
            size,
            created: new Date(),
            id: generateId(),
          }),
          ...(reviewsState?.data ?? []),
        ],
      }
    );
  };

  const onDeleteReview = () => {
    reviewsState.mutate(
      async () => {
        if (selectedReview?.id) {
          await source.deleteCohortReview(
            params.studyId,
            cohort.id,
            selectedReview.id
          );
          navigate(absoluteCohortReviewListURL(params, cohort.id));
        }
        return wrapResults(
          await source.listCohortReviews(params.studyId, cohort.id)
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
          await source.renameCohortReview(
            params.studyId,
            cohort.id,
            selectedReview.id,
            name
          );
        }
        return wrapResults(
          await source.listCohortReviews(params.studyId, cohort.id)
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
    onCreate: onCreateNewReview,
  });

  const [renameReviewDialog, showRenameReviewDialog] = useTextInputDialog({
    title: `Rename ${selectedReview?.displayName}`,
    initialText: selectedReview?.displayName,
    textLabel: "Review name",
    buttonLabel: "Rename",
    onConfirm: onRenameReview,
  });

  return (
    <GridLayout rows>
      <ActionBar
        title={`Reviews for cohort ${cohort.name}`}
        backURL={absoluteCohortURL(params, cohort.id)}
      />
      <Box
        sx={{
          width: "100%",
          height: "100%",
          display: "grid",
          gridTemplateColumns: "280px 1fr",
          gridTemplateRows: "1fr",
          gridTemplateAreas: "'reviews stats'",
        }}
      >
        <Box
          sx={{
            gridArea: "reviews",
            p: 1,
          }}
        >
          <GridLayout rows>
            <GridLayout cols height="auto">
              <Typography variant="h6" sx={{ mr: 1 }}>
                Reviews
              </Typography>
              <IconButton onClick={showNewReviewDialog}>
                <AddIcon />
              </IconButton>
              <GridBox />
            </GridLayout>
            {!!reviewsState.data?.length ? (
              <List sx={{ p: 0 }}>
                {reviewsState.data?.map((item) => (
                  <ListItemButton
                    sx={{ p: 0, mt: 1 }}
                    component={RouterLink}
                    key={item.id()}
                    to={absoluteCohortReviewListURL(
                      params,
                      cohort.id,
                      item.id()
                    )}
                    disabled={!!item.pending}
                  >
                    <SelectablePaper
                      selected={item.id() === selectedReview?.id}
                    >
                      <Stack
                        direction="row"
                        justifyContent="space-between"
                        sx={{ p: 1 }}
                      >
                        <Stack>
                          <Typography variant="body1em">
                            {item.displayName()}
                          </Typography>
                          <Typography variant="body1">
                            {item.created()?.toLocaleString()}
                          </Typography>
                          <Typography variant="body1">
                            {`Participants: ${item.size()}`}
                          </Typography>
                        </Stack>
                        <Typography variant="body1">
                          {!!item.pending ? (
                            <CircularProgress
                              sx={{ maxWidth: "1em", maxHeight: "1em" }}
                            />
                          ) : null}
                        </Typography>
                      </Stack>
                    </SelectablePaper>
                  </ListItemButton>
                ))}
              </List>
            ) : (
              <Empty
                maxWidth="90%"
                minHeight="200px"
                title="No reviews created"
              />
            )}
          </GridLayout>
        </Box>
        <Box
          sx={{
            gridArea: "stats",
            backgroundColor: (theme) => theme.palette.background.paper,
            p: 1,
          }}
        >
          {!!selectedReview ? (
            <Stack>
              <Stack direction="row">
                <Typography variant="h6" sx={{ mr: 1 }}>
                  {selectedReview.displayName}
                </Typography>
                <IconButton onClick={showRenameReviewDialog}>
                  <EditIcon />
                </IconButton>
                <IconButton onClick={onDeleteReview}>
                  <DeleteIcon />
                </IconButton>
              </Stack>
              <ReviewStats review={selectedReview} />
            </Stack>
          ) : (
            <Empty
              maxWidth="60%"
              minHeight="300px"
              image="/empty.svg"
              title="No reviews created"
              subtitle="You can create a review by clicking on the '+' button"
            />
          )}
        </Box>
        <Box sx={{ gridArea: "1/1/-1/-1" }}>
          {reviewsState.isLoading ? <LoadingOverlay /> : undefined}
        </Box>
        {newReviewDialog}
        {renameReviewDialog}
      </Box>
    </GridLayout>
  );
}

function ReviewStats(props: { review: CohortReview }) {
  const navigate = useNavigate();

  return (
    <GridLayout rows width="auto">
      <Button variant="contained" onClick={() => navigate("review")}>
        Review
      </Button>
      <GridBox sx={{ p: 2 }}>
        <DemographicCharts cohort={props.review.cohort} />
      </GridBox>
    </GridLayout>
  );
}
