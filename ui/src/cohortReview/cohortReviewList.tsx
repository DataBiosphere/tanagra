import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ActionBar from "actionBar";
import Empty from "components/empty";
import LoadingOverlay from "components/loadingOverlay";
import SelectablePaper from "components/selectablePaper";
import { useTextInputDialog } from "components/textInputDialog";
import { useSource } from "data/source";
import { CohortReview } from "data/types";
import { useCohort, useUnderlay } from "hooks";
import { useCallback, useEffect } from "react";
import { Link as RouterLink, useNavigate, useParams } from "react-router-dom";
import { absoluteCohortURL, cohortReviewURL } from "router";
import { useImmer } from "use-immer";
import { useNewReviewDialog } from "./newReviewDialog";

type State = {
  loading: boolean;
  reviews?: CohortReview[];
};

export function CohortReviewList() {
  const source = useSource();
  const underlay = useUnderlay();
  const cohort = useCohort();
  const navigate = useNavigate();
  const { reviewId } = useParams<{ reviewId: string }>();

  const [state, updateState] = useImmer<State>({ loading: true });

  const selectedReview = state.reviews?.find(
    (r, i) => r.id === reviewId || (!reviewId && i === 0)
  );

  const loadReviews = useCallback(async () => {
    const reviews = await source.listCohortReviews(cohort.id);
    updateState((state) => {
      state.loading = false;
      state.reviews = reviews;
    });
  }, [cohort]);

  useEffect(() => {
    loadReviews();
  }, [cohort]);

  const onCreateNewReview = (name: string, size: number) => {
    updateState((state) => {
      state.loading = true;
    });
    const create = async () => {
      const review = await source.createCohortReview(name, size, cohort);

      await loadReviews();

      navigate(cohortReviewURL(underlay.name, cohort.id, review.id));
    };
    create();
  };

  const onDeleteReview = () => {
    if (!selectedReview?.id) {
      throw new Error("No review selected to delete");
    }

    updateState((state) => {
      state.loading = true;
    });
    const del = async () => {
      await source.deleteCohortReview(selectedReview.id, cohort.name);
      await loadReviews();
      navigate(cohortReviewURL(underlay.name, cohort.id));
    };
    del();
  };

  const onRenameReview = (name: string) => {
    updateState((state) => {
      state.loading = true;
    });
    const rename = async () => {
      if (!selectedReview?.id) {
        throw new Error("No review selected to rename");
      }

      await source.renameCohortReview(name, selectedReview.id, cohort.name);
      await loadReviews();
    };
    rename();
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
          {!!state.reviews?.length ? (
            <List sx={{ p: 0 }}>
              {state.reviews?.map((review) => (
                <ListItemButton
                  sx={{ p: 0, mt: 1 }}
                  component={RouterLink}
                  key={review.id}
                  to={cohortReviewURL(underlay.name, cohort.id, review.id)}
                >
                  <SelectablePaper selected={review.id === selectedReview?.id}>
                    <Stack sx={{ p: 1 }}>
                      <Typography variant="h4">{review.displayName}</Typography>
                      <Typography variant="body2">
                        {review.created.toLocaleString()}
                      </Typography>
                      <Typography variant="body2">{`Participants: ${review.size}`}</Typography>
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
          {state.loading ? <LoadingOverlay /> : undefined}
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
