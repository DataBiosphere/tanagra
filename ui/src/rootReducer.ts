import { combineReducers, createAction } from "@reduxjs/toolkit";
import cohortsReducer from "cohortsSlice";
import conceptSetsReducer from "conceptSetsSlice";
import { AnyAction, Reducer } from "redux";
import undoable from "redux-undo";
import * as tanagra from "tanagra-api";
import underlaysReducer from "underlaysSlice";
import urlSlice from "urlSlice";

const slicesReducer = combineReducers({
  cohorts: undoable(cohortsReducer),
  underlays: underlaysReducer,
  conceptSets: undoable(conceptSetsReducer),
  url: undoable(urlSlice),
});

export type RootState = ReturnType<typeof slicesReducer>;

export const loadUserData = createAction<tanagra.UserData>("loadUserData");

export const rootReducer: Reducer = (state: RootState, action: AnyAction) => {
  console.log(state)
  if (loadUserData.match(action)) {
    return {
      ...state,
      cohorts: {
        ...state.cohorts,
        present: action.payload.cohorts
      },
      conceptSets: {
        ...state.conceptSets,
        present: action.payload.conceptSets
      },
    };
  }
  return slicesReducer(state, action);
};
