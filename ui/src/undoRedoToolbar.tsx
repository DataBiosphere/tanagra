import RedoIcon from "@mui/icons-material/Redo";
import UndoIcon from "@mui/icons-material/Undo";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import { createContext, useContext } from "react";
import { RouterLink } from "util/searchState";

export type UndoRedoData = {
  undoURL: string;
  redoURL: string;
  undoAction?: () => void;
  redoAction?: () => void;
};

export const UndoRedoContext = createContext<UndoRedoData | null>(null);

export default function UndoRedoToolbar() {
  const data = useContext(UndoRedoContext);
  if (!data) {
    throw new Error("No UndoRedoData available.");
  }

  return (
    <Stack direction="row" spacing={1}>
      <Button
        onClick={() => data.undoAction?.()}
        variant="outlined"
        size="large"
        startIcon={<UndoIcon fontSize="small" />}
        disabled={!data.undoAction}
        component={RouterLink}
        to={data.undoURL}
      >
        Undo
      </Button>
      <Button
        onClick={() => data.redoAction?.()}
        variant="outlined"
        size="large"
        startIcon={<RedoIcon fontSize="small" />}
        disabled={!data.redoAction}
        component={RouterLink}
        to={data.redoURL}
      >
        Redo
      </Button>
    </Stack>
  );
}
