import RateReviewIcon from "@mui/icons-material/RateReview";
import RedoIcon from "@mui/icons-material/Redo";
import UndoIcon from "@mui/icons-material/Undo";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import {
  useAppDispatch,
  useAppSelector,
  useCohort,
  useUndoRedoUrls,
} from "hooks";
import { Link as RouterLink } from "react-router-dom";
import { ActionCreators as UndoActionCreators } from "redux-undo";
import { absoluteCohortReviewURL, useBaseParams } from "router";

export default function CohortToolbar() {
  const cohort = useCohort();
  const params = useBaseParams();

  const dispatch = useAppDispatch();
  const canUndo = useAppSelector((state) => {
    if (state.past.length === 0) {
      return false;
    }

    // TODO(tjennison): Eventually undo/redo will be contained within individual
    // cohorts/concept sets but for now prevent undo across them. This isn't
    // necessary in canRedo because they can't be undone in the first place.
    const past = state.past[state.past.length - 1];
    if (
      state.present.cohorts.length !== past.cohorts.length ||
      state.present.conceptSets.length !== past.conceptSets.length
    ) {
      return false;
    }

    return true;
  });
  const canRedo = useAppSelector((state) => state.future.length > 0);
  const [undoUrlPath, redoUrlPath] = useUndoRedoUrls();

  return (
    <Stack direction="row" spacing={1} sx={{ m: 1 }}>
      <Button
        onClick={() => dispatch(UndoActionCreators.undo())}
        variant="outlined"
        startIcon={<UndoIcon />}
        disabled={!canUndo}
        component={RouterLink}
        to={undoUrlPath}
      >
        Undo
      </Button>
      <Button
        onClick={() => dispatch(UndoActionCreators.redo())}
        variant="outlined"
        startIcon={<RedoIcon />}
        disabled={!canRedo}
        component={RouterLink}
        to={redoUrlPath}
      >
        Redo
      </Button>
      <Divider orientation="vertical" flexItem />
      <Button
        startIcon={<RateReviewIcon />}
        component={RouterLink}
        to={absoluteCohortReviewURL(params, cohort.id)}
      >
        Review
      </Button>
    </Stack>
  );
}
