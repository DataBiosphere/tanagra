import { combineReducers, createAction } from "@reduxjs/toolkit";
import cohortsReducer from "cohortsSlice";
import conceptSetsReducer from "conceptSetsSlice";
import { AnyAction, Reducer } from "redux";
import undoable from "redux-undo";
import * as tanagra from "tanagra-api";
import underlaysReducer from "underlaysSlice";
import urlsSlice from "urlsSlice";

const slicesReducer = combineReducers({
  cohorts: cohortsReducer,
  underlays: underlaysReducer,
  conceptSets: conceptSetsReducer,
  urls: urlsSlice,
});

const undoableSlicesReducer = undoable(slicesReducer);

export type RootState = ReturnType<typeof undoableSlicesReducer>;

export const loadUserData = createAction<tanagra.UserData>("loadUserData");

export const rootReducer: Reducer = (state: RootState, action: AnyAction) => {
  if (loadUserData.match(action)) {
    return {
      ...state,
      present: {
        ...state.present,
        cohorts: action.payload.cohorts,
        conceptSets: action.payload.conceptSets,
      },
    };
  }
  return undoableSlicesReducer(state, action);
};
