import RedoIcon from "@mui/icons-material/Redo";
import UndoIcon from "@mui/icons-material/Undo";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import {
  useCohort,
  useRedoAction,
  useUndoAction,
  useUndoRedoUrls,
} from "hooks";
import { Link as RouterLink } from "react-router-dom";
import { absoluteCohortReviewListURL, useBaseParams } from "router";

export default function CohortToolbar() {
  const cohort = useCohort();
  const params = useBaseParams();

  const [undoUrlPath, redoUrlPath] = useUndoRedoUrls();
  const undo = useUndoAction();
  const redo = useRedoAction();

  return (
    <Stack direction="row" spacing={1}>
      <Button
        onClick={() => undo?.()}
        startIcon={<UndoIcon />}
        disabled={!undo}
        component={RouterLink}
        to={undoUrlPath}
      >
        Undo
      </Button>
      <Button
        onClick={() => redo?.()}
        startIcon={<RedoIcon />}
        disabled={!redo}
        component={RouterLink}
        to={redoUrlPath}
      >
        Redo
      </Button>
      <Divider orientation="vertical" flexItem />
      <Button
        variant="outlined"
        component={RouterLink}
        to={absoluteCohortReviewListURL(params, cohort.id)}
      >
        Review cohort
      </Button>
    </Stack>
  );
}
