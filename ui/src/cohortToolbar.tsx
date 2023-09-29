import RedoIcon from "@mui/icons-material/Redo";
import UndoIcon from "@mui/icons-material/Undo";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import { useRedoAction, useUndoAction, useUndoRedoUrls } from "hooks";
import { RouterLink } from "util/searchState";

export default function CohortToolbar() {
  const [undoUrlPath, redoUrlPath] = useUndoRedoUrls();
  const undo = useUndoAction();
  const redo = useRedoAction();

  return (
    <Stack direction="row" spacing={1}>
      <Button
        onClick={() => undo?.()}
        variant="outlined"
        size="large"
        startIcon={<UndoIcon fontSize="small" />}
        disabled={!undo}
        component={RouterLink}
        to={undoUrlPath}
      >
        Undo
      </Button>
      <Button
        onClick={() => redo?.()}
        variant="outlined"
        size="large"
        startIcon={<RedoIcon fontSize="small" />}
        disabled={!redo}
        component={RouterLink}
        to={redoUrlPath}
      >
        Redo
      </Button>
    </Stack>
  );
}
