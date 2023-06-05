import RedoIcon from "@mui/icons-material/Redo";
import UndoIcon from "@mui/icons-material/Undo";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import { useRedoAction, useUndoAction, useUndoRedoUrls } from "hooks";
import { Link as RouterLink } from "react-router-dom";

export default function CohortToolbar() {
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
    </Stack>
  );
}
