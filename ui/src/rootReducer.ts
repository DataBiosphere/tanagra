import { combineReducers, createAction } from "@reduxjs/toolkit";
import cohortsReducer from "cohortsSlice";
import conceptSetsReducer from "conceptSetsSlice";
import { AnyAction, Reducer } from "redux";
import undoable, { excludeAction } from "redux-undo";
import * as tanagra from "tanagra-api";
import underlaysReducer from "underlaysSlice";
import urlSlice from "urlSlice";

export const loadUserData = createAction<tanagra.UserData>("loadUserData");

const undoableConfigs = {
  initTypes: [loadUserData.type],
  filter: excludeAction(["underlays/setUnderlays"]),
};

const slicesReducer = combineReducers({
  cohorts: undoable(cohortsReducer, undoableConfigs),
  underlays: underlaysReducer,
  conceptSets: undoable(conceptSetsReducer, undoableConfigs),
  url: undoable(urlSlice, undoableConfigs),
});

export type RootState = ReturnType<typeof slicesReducer>;

export const rootReducer: Reducer = (state: RootState, action: AnyAction) => {
  if (loadUserData.match(action)) {
    return {
      ...state,
      cohorts: {
        ...state.cohorts,
        present: action.payload.cohorts,
      },
      conceptSets: {
        ...state.conceptSets,
        present: action.payload.conceptSets,
      },
    };
  }
  return slicesReducer(state, action);
};
