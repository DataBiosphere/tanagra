import RedoIcon from "@mui/icons-material/Redo";
import UndoIcon from "@mui/icons-material/Undo";
import { Box, Button } from "@mui/material";
import { useAppDispatch, useAppSelector, useUndoRedoUrls } from "hooks";
import { Link as RouterLink } from "react-router-dom";
import { ActionCreators as UndoActionCreators } from "redux-undo";

function UndoRedo() {
  const dispatch = useAppDispatch();
  const canUndo = useAppSelector((state) => state.past.length > 0);
  const canRedo = useAppSelector((state) => state.future.length > 0);
  const [undoUrlPath, redoUrlPath] = useUndoRedoUrls();

  return (
    <Box>
      <Button
        onClick={() => dispatch(UndoActionCreators.undo())}
        disabled={!canUndo}
        component={RouterLink}
        to={undoUrlPath}
      >
        <UndoIcon
          fontSize="medium"
          sx={{ color: canUndo ? "white" : "gray" }}
        />
      </Button>
      <Button
        onClick={() => dispatch(UndoActionCreators.redo())}
        disabled={!canRedo}
        component={RouterLink}
        to={redoUrlPath}
      >
        <RedoIcon
          fontSize="medium"
          sx={{ color: canRedo ? "white" : "gray" }}
        />
      </Button>
    </Box>
  );
}

export default UndoRedo;
