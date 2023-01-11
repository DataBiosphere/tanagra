import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import { generateId } from "cohort";
import Empty from "components/empty";
import LoadingOverlay from "components/loadingOverlay";
import SelectablePaper from "components/selectablePaper";
import { useTextInputDialog } from "components/textInputDialog";
import { useSource } from "data/source";
import { CohortReview } from "data/types";
import { useCohort, useUnderlay } from "hooks";
import produce from "immer";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";
import { absoluteCohortURL, cohortReviewURL } from "router";
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

export function CohortReviewList() {
  const source = useSource();
  const underlay = useUnderlay();
  const cohort = useCohort();
  const navigate = useNavigate();
  const { reviewId } = useParams<{ reviewId: string }>();

  const reviewsState = useSWR(
    { component: "CohortReviewList", cohortId: cohort.id },
    async () => {
      return wrapResults(await source.listCohortReviews(cohort.id));
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
        await source.createCohortReview(name, size, cohort);
        return wrapResults(await source.listCohortReviews(cohort.id));
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
          await source.deleteCohortReview(selectedReview.id, cohort.name);
          navigate(cohortReviewURL(underlay.name, cohort.id));
        }
        return wrapResults(await source.listCohortReviews(cohort.id));
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
          await source.renameCohortReview(name, selectedReview.id, cohort.name);
        }
        return wrapResults(await source.listCohortReviews(cohort.id));
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
    <>
      <ActionBar
        title={`Reviews of ${cohort.name}`}
        backURL={absoluteCohortURL(underlay.name, cohort.id)}
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
          <Stack direction="row">
            <Typography variant="h2" sx={{ mr: 1 }}>
              Reviews
            </Typography>
            <IconButton onClick={showNewReviewDialog}>
              <AddIcon />
            </IconButton>
          </Stack>
          {!!reviewsState.data?.length ? (
            <List sx={{ p: 0 }}>
              {reviewsState.data?.map((item) => (
                <ListItemButton
                  sx={{ p: 0, mt: 1 }}
                  component={RouterLink}
                  key={item.id()}
                  to={cohortReviewURL(underlay.name, cohort.id, item.id())}
                  disabled={!!item.pending}
                >
                  <SelectablePaper selected={item.id() === selectedReview?.id}>
                    <Stack
                      direction="row"
                      justifyContent="space-between"
                      sx={{ p: 1 }}
                    >
                      <Stack>
                        <Typography variant="h4">
                          {item.displayName()}
                        </Typography>
                        <Typography variant="body2">
                          {item.created()?.toLocaleString()}
                        </Typography>
                        <Typography variant="body2">
                          {`Participants: ${item.size()}`}
                        </Typography>
                      </Stack>
                      <Typography variant="h4">
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
                <Typography variant="h2" sx={{ mr: 1 }}>
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
              image="/empty.png"
              title="No reviews created"
              subtitle="You can create a review by clicking on the '+' button"
            />
          )}
        </Box>
        <Box sx={{ gridArea: "1/1/-1/-1" }}>
          {reviewsState.isLoading ? <LoadingOverlay /> : undefined}
        </Box>
      </Box>
      {newReviewDialog}
      {renameReviewDialog}
    </>
  );
}

function ReviewStats(props: { review: CohortReview }) {
  return (
    <Stack>
      <Typography variant="body1">
        TODO: Statistics for {props.review.displayName}!
      </Typography>
    </Stack>
  );
}
