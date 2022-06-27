import React from "react";
import { ActionCreators as UndoActionCreators } from 'redux-undo'
import { connect, ConnectedProps } from 'react-redux'
import { Box, Button } from "@mui/material";
import UndoIcon from "@mui/icons-material/Undo";
import RedoIcon from "@mui/icons-material/Redo";
import { RootState } from "rootReducer";
import { AppDispatch } from "store";

const mapStateToProps = (state: RootState) => {
    console.log(state);
    return {
        canUndo: state.past.length > 0,
        canRedo: state.future.length > 0,
    }
}

const mapDispatchToProps = (dispatch: AppDispatch) => {
    return {
        onUndo: () => dispatch(UndoActionCreators.undo()),
        onRedo: () => dispatch(UndoActionCreators.redo())
    }
}

const connector = connect(mapStateToProps, mapDispatchToProps)

type propsFromRedux = ConnectedProps<typeof connector>

function UndoRedo({
    canUndo,
    canRedo,
    onUndo,
    onRedo,
}: propsFromRedux) {
    return (
        <Box>
            <Button onClick={onUndo}  disabled={!canUndo}>
                <UndoIcon fontSize="medium" sx={{ color: canUndo ? "white" : "gray" }} />
            </Button>
            <Button onClick={onRedo}  disabled={!canRedo}>
                <RedoIcon fontSize="medium" sx={{ color: canRedo ? "white" : "gray" }} />
            </Button>
        </Box>
    )
}

export default connector(UndoRedo);
