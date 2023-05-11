import { combineReducers, createAction } from "@reduxjs/toolkit";
import { AnyAction, Reducer } from "redux";
import undoable from "redux-undo";
import * as tanagra from "tanagra-api";
import underlaysReducer, { setUnderlays } from "underlaysSlice";
import urlSlice from "urlSlice";

export const loadUserData = createAction<tanagra.UserData>("loadUserData");

const undoableConfigs = {
  initTypes: [loadUserData.type, setUnderlays.type],
};

const slicesReducer = combineReducers({
  underlays: underlaysReducer,
  url: urlSlice,
});

const undoableSlicesReducer = undoable(slicesReducer, undoableConfigs);

export type RootState = ReturnType<typeof undoableSlicesReducer>;

export const rootReducer: Reducer = (state: RootState, action: AnyAction) => {
  if (loadUserData.match(action)) {
    return {
      ...state,
      present: {
        ...state.present,
      },
    };
  } else if (setUnderlays.match(action)) {
    return {
      ...state,
      present: {
        ...state.present,
        underlays: action.payload,
      },
    };
  }
  return undoableSlicesReducer(state, action);
};
