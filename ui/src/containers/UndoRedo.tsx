import RedoIcon from "@mui/icons-material/Redo";
import UndoIcon from "@mui/icons-material/Undo";
import { Box, Button } from "@mui/material";
import { useUrl } from "hooks";
import { connect, ConnectedProps } from "react-redux";
import { Link as RouterLink } from "react-router-dom";
import { ActionCreators as UndoActionCreators } from "redux-undo";
import { RootState } from "rootReducer";
import { AppDispatch } from "store";

const mapStateToProps = (state: RootState) => {
  return {
    canUndo: state.url.past.length > 0,
    canRedo: state.url.future.length > 0,
  };
};

const mapDispatchToProps = (dispatch: AppDispatch) => {
  return {
    onUndo: () => dispatch(UndoActionCreators.undo()),
    onRedo: () => dispatch(UndoActionCreators.redo()),
  };
};

const connector = connect(mapStateToProps, mapDispatchToProps);

type propsFromRedux = ConnectedProps<typeof connector>;

function UndoRedo({ canUndo, canRedo, onUndo, onRedo }: propsFromRedux) {
  const url = useUrl();

  return (
    <Box>
      <Button
        onClick={onUndo}
        disabled={!canUndo}
        component={RouterLink}
        to={url}
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
        to={url}
      >
        <RedoIcon
          fontSize="medium"
          sx={{ color: canRedo ? "white" : "gray" }}
        />
      </Button>
    </Box>
  );
}

export default connector(UndoRedo);
