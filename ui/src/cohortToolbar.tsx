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
  useUnderlay,
  useUndoRedoUrls,
} from "hooks";
import { Link as RouterLink } from "react-router-dom";
import { ActionCreators as UndoActionCreators } from "redux-undo";
import { cohortReviewURL } from "router";

export default function CohortToolbar() {
  const underlay = useUnderlay();
  const cohort = useCohort();

  const dispatch = useAppDispatch();
  const canUndo = useAppSelector((state) => state.past.length > 0);
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
        variant="contained"
        startIcon={<RateReviewIcon />}
        component={RouterLink}
        to={cohortReviewURL(underlay.name, cohort.id)}
      >
        Review
      </Button>
    </Stack>
  );
}
