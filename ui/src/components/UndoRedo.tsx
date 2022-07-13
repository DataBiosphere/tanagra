import RedoIcon from "@mui/icons-material/Redo";
import UndoIcon from "@mui/icons-material/Undo";
import { Box, Button } from "@mui/material";
import { useAppDispatch, useAppSelector, useUrl } from "hooks";
import { Link as RouterLink } from "react-router-dom";
import { ActionCreators as UndoActionCreators } from "redux-undo";

function UndoRedo() {
  const urlHistory = useUrl();
  const dispatch = useAppDispatch();

  const canUndo = useAppSelector((state) => state.url.past.length > 0);
  const canRedo = useAppSelector((state) => state.url.future.length > 0);

  const onUndo = () => dispatch(UndoActionCreators.undo());
  const onRedo = () => dispatch(UndoActionCreators.redo());

  return (
    <Box>
      <Button
        onClick={onUndo}
        disabled={!canUndo}
        component={RouterLink}
        to={urlHistory.present}
      >
        <UndoIcon
          fontSize="medium"
          sx={{ color: canUndo ? "white" : "gray" }}
        />
      </Button>
      <Button
        onClick={onRedo}
        disabled={!canRedo}
        component={RouterLink}
        to={urlHistory.future[0]}
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
